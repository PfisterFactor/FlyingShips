package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 3/12/2016.
  */
class BlockChangedMessage(ship: EntityShip, changedBlock: BlockPos) extends IMessage {
  def this() = this(null, new BlockPos(-1, -1, -1))

  var ShipID = if (ship != null) ship.getEntityId else -1
  var ChangedBlock = changedBlock
  var Blockstate: IBlockState = if (ship != null) ship.Shipworld.getBlockState(changedBlock) else Blocks.air.getDefaultState
  var ChangedTileEntityNBT: NBTTagCompound = if (ship != null && ship.Shipworld.getTileEntity(ChangedBlock) != null) {
    val nbt = new NBTTagCompound()
    ship.Shipworld.getTileEntity(ChangedBlock).writeToNBT(nbt)
    nbt
  } else null
  var HasTileEntity = ChangedTileEntityNBT != null


  override def toBytes(buf: ByteBuf): Unit = {

    // ShipID
    buf.writeInt(ShipID)


    // ChangedBlock
    buf.writeLong(changedBlock.toLong)

    // Blockstate
    buf.writeInt(Block.getStateId(Blockstate))

    // HasTileEntity
    buf.writeBoolean(HasTileEntity)

    // TileEntity
    ByteBufUtils.writeTag(buf, ChangedTileEntityNBT)
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // ShipID
    ShipID = buf.readInt()

    // Changed Block
    ChangedBlock = BlockPos.fromLong(buf.readLong())

    // Blockstate
    Blockstate = Block.getStateById(buf.readInt())

    // HasTileEntity
    HasTileEntity = buf.readBoolean()

    // TileEntity
    if (HasTileEntity)
      ChangedTileEntityNBT = ByteBufUtils.readTag(buf)
    else
      ChangedTileEntityNBT = null

  }


}

class ClientBlockChangedMessageHandler extends IMessageHandler[BlockChangedMessage, IMessage] {

  @Override
  override def onMessage(message: BlockChangedMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID == -1) return null

    Minecraft.getMinecraft.addScheduledTask(new BlocksChangedMessageTask(message, ctx))
    null
  }

  class BlocksChangedMessageTask(message: BlockChangedMessage, ctx: MessageContext) extends Runnable {
    val Message = message
    val Context = ctx

    // On Client
    override def run(): Unit = {


      def player = Minecraft.getMinecraft.thePlayer

      val Ship = ShipLocator.getShip(player.worldObj, message.ShipID)

      if (Ship.isEmpty)
        return


      Ship.get.Shipworld.applyBlockChange(message.ChangedBlock, message.Blockstate, 3)

      if (message.HasTileEntity && message.ChangedTileEntityNBT != null) {
        val teOnShip = Ship.get.Shipworld.getTileEntity(message.ChangedBlock)
        if (teOnShip != null)
          teOnShip.readFromNBT(message.ChangedTileEntityNBT)
        else {
          val te = TileEntity.createAndLoadEntity(message.ChangedTileEntityNBT)
          te.setWorldObj(Ship.get.Shipworld)
          te.validate()
          Ship.get.Shipworld.addTileEntity(te)
        }
      }
    }
  }

}
