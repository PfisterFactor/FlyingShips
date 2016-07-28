package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 7/20/2016.
  */
class BlockActionMessage(blockPos: BlockPos, block: Block, data1: Int, data2: Int, shipID: Int) extends IMessage {
  def this() = this(new BlockPos(0, 0, 0), Block.getBlockById(0), 0, 0, -1)

  var ShipID: Int = shipID
  var BlockPosition: BlockPos = blockPos
  var Data1: Int = data1
  var Data2: Int = data2
  var BlockOf: Block = block

  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeInt(shipID)

    // BlockPosition
    buf.writeLong(BlockPosition.toLong)

    // Data1
    buf.writeInt(Data1)

    // Data2
    buf.writeInt(Data2)

    // Block
    buf.writeInt(Block.getIdFromBlock(BlockOf) & 4095)

  }

  override def fromBytes(buf: ByteBuf): Unit = {

    // ShipID
    ShipID = buf.readInt()

    // BlockPosition
    BlockPosition = BlockPos.fromLong(buf.readLong())

    // Data1
    Data1 = buf.readInt()

    // Data2
    Data2 = buf.readInt()

    // Block
    BlockOf = Block.getBlockById(buf.readInt())
  }
}

class ClientBlockActionMessageHandler extends IMessageHandler[BlockActionMessage, IMessage] {
  override def onMessage(message: BlockActionMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID == -1) return null
    Minecraft.getMinecraft.addScheduledTask(new BlockActionMessageTask(message, ctx))
    null
  }

  class BlockActionMessageTask(message: BlockActionMessage, ctx: MessageContext) extends Runnable {
    override def run(): Unit = {
      val ship = ShipLocator.getShip(Minecraft.getMinecraft.theWorld, message.ShipID)

      if (ship.isEmpty || ship.get.Shipworld == null) {
        println(s"Empty or null ship in block action message task, ID: ${message.ShipID}!")
        return
      }
      ship.get.Shipworld.addBlockEvent(message.BlockPosition, message.BlockOf, message.Data1, message.Data2)
    }
  }

}
