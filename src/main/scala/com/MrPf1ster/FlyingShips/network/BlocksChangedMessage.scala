package com.MrPf1ster.FlyingShips.network

import com.MrPf1ster.FlyingShips.entities.EntityShip
import com.MrPf1ster.FlyingShips.util.ShipLocator
import io.netty.buffer.ByteBuf
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 3/12/2016.
  */
class BlocksChangedMessage(ship: EntityShip, changedBlocks: Array[BlockPos]) extends IMessage {
  def this() = this(null, Array())

  var ShipID = if (ship != null) ship.getEntityId else -1
  var ChangedBlocks = changedBlocks
  var BlockStates: Array[IBlockState] = if (ship != null) ChangedBlocks.map(pos => ship.ShipWorld.getBlockState(pos)) else Array()
  var NumChangedBlocks = ChangedBlocks.length
  var ChangedTileEntities: Array[TileEntity] = if (ship != null)
    changedBlocks.map( pos => Option(ship.ShipWorld.getTileEntity(pos))).filter(opt => opt.isDefined).map(opt => opt.get)
  else Array()
  var NumChangedTileEntities = ChangedTileEntities.length



  override def toBytes(buf: ByteBuf): Unit = {

    // ShipID
    buf.writeInt(ShipID)


    // NumChangedBlocks
    buf.writeInt(NumChangedBlocks)

    // ChangedBlocks
    ChangedBlocks.foreach(pos => buf.writeLong(pos.toLong))

    // BlockStates
    BlockStates.foreach(state => buf.writeInt(Block.BLOCK_STATE_IDS.get(state)))

    // TileEntities Length
    buf.writeInt(NumChangedTileEntities)

    // TileEntities
    ChangedTileEntities.foreach(te => {
      val nbt = new NBTTagCompound
      te.writeToNBT(nbt)
      ByteBufUtils.writeTag(buf,nbt)
    })
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // ShipID
    ShipID = buf.readInt()

    // NumChangedBlocks
    NumChangedBlocks = buf.readInt()

    // ChangedBlocks
    for (i <- 0 until NumChangedBlocks) {
      ChangedBlocks = ChangedBlocks :+ BlockPos.fromLong(buf.readLong())
    }

    // BlockStates
    for (i <- 0 until NumChangedBlocks) {
      BlockStates = BlockStates :+ Block.BLOCK_STATE_IDS.getByValue(buf.readInt())
    }

    // Num Changed Tile Entities
    NumChangedTileEntities = buf.readInt()

    for (i <- 0 until NumChangedTileEntities) {
      ChangedTileEntities = ChangedTileEntities.:+(TileEntity.createAndLoadEntity(ByteBufUtils.readTag(buf)))
    }



  }


}

class ClientBlocksChangedMessageHandler extends IMessageHandler[BlocksChangedMessage, IMessage] {

  @Override
  override def onMessage(message: BlocksChangedMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID == -1) return null

    Minecraft.getMinecraft.addScheduledTask(new BlocksChangedMessageTask(message, ctx))
    null
  }

  class BlocksChangedMessageTask(message: BlocksChangedMessage, ctx: MessageContext) extends Runnable {
    val Message = message
    val Context = ctx

    // On Client
    override def run(): Unit = {


      def player = Minecraft.getMinecraft.thePlayer

      val Ship = ShipLocator.getShip(player.worldObj,message.ShipID)

      if (Ship.isEmpty)
        return


      for (i <- 0 until Message.NumChangedBlocks)
        Ship.get.ShipWorld.applyBlockChange(Message.ChangedBlocks(i), Message.BlockStates(i), 3)

      message.ChangedTileEntities.foreach(te => Ship.get.ShipWorld.addTileEntity(te))
    }
  }

}
