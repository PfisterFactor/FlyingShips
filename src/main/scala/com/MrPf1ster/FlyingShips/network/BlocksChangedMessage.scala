package com.MrPf1ster.FlyingShips.network

import com.MrPf1ster.FlyingShips.entities.ShipEntity
import com.google.common.base.Predicates
import io.netty.buffer.ByteBuf
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 3/12/2016.
  */
class BlocksChangedMessage(ship: ShipEntity, blocksChanged: Array[BlockPos]) extends IMessage {
  def this() = this(null, Array())

  var ChangedBlocks = blocksChanged
  var ShipID = if (ship != null) ship.ShipID else -1
  var numChangedBlocks = ChangedBlocks.length
  var BlockStates: Array[IBlockState] = if (ship != null) ChangedBlocks.map(pos => ship.ShipWorld.getBlockState(pos)) else Array()


  override def toBytes(buf: ByteBuf): Unit = {
    buf.writeInt(ShipID)
    buf.writeInt(numChangedBlocks)
    ChangedBlocks.foreach(pos => buf.writeLong(pos.toLong))
    BlockStates.foreach(state => buf.writeInt(Block.BLOCK_STATE_IDS.get(state)))
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    ShipID = buf.readInt()
    numChangedBlocks = buf.readInt()
    for (i <- 0 until numChangedBlocks) {
      ChangedBlocks = ChangedBlocks :+ BlockPos.fromLong(buf.readLong())
    }
    for (i <- 0 until numChangedBlocks) {
      BlockStates = BlockStates :+ Block.BLOCK_STATE_IDS.getByValue(buf.readInt())
    }


  }


}

class ClientBlocksChangedMessageHandler extends IMessageHandler[BlocksChangedMessage, IMessage] {
  println("created message handler")

  @Override
  override def onMessage(message: BlocksChangedMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID == -1) return null

    Minecraft.getMinecraft.addScheduledTask(new BlocksChangedMessageTask(message, ctx))
    println("Message received")
    null
  }

  class BlocksChangedMessageTask(message: BlocksChangedMessage, ctx: MessageContext) extends Runnable {
    val Message = message
    val Context = ctx

    override def run(): Unit = {
      val thePlayer = Minecraft.getMinecraft.thePlayer

      def world = thePlayer.getEntityWorld

      val entities = world.getEntities(classOf[ShipEntity], Predicates.alwaysTrue[ShipEntity])
      val iter = entities.iterator()

      var Ship: ShipEntity = null
      while (iter.hasNext) {
        val next = iter.next()
        if (next.ShipID == Message.ShipID)
          Ship = next
      }

      for (i <- 0 until Message.numChangedBlocks) {
        Ship.ShipWorld.applyBlockChange(Message.ChangedBlocks(i), Message.BlockStates(i), 3)
      }
    }
  }

}
