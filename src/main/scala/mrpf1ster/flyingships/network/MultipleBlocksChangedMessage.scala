package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.util.BlockPos
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 7/29/2016.
  */
class MultipleBlocksChangedMessage(shipID: Int, size: Int, crammedPositions: Array[Short], chunk: Chunk) extends IMessage {
  def this() = this(-1, -1, Array(), null)

  var ShipID = shipID
  var ChunkPosCoord: ChunkCoordIntPair = if (chunk != null) new ChunkCoordIntPair(chunk.xPosition, chunk.zPosition) else null
  var ChangedBlocks: Array[BlockUpdateData] = (0 until size).map(i => new BlockUpdateData(crammedPositions(i), chunk)).toArray

  override def toBytes(buf: ByteBuf): Unit = {
    // ChunkPosCoord
    buf.writeInt(ChunkPosCoord.chunkXPos)
    buf.writeInt(ChunkPosCoord.chunkZPos)

    // ChangedBlocks
    buf.writeInt(ChangedBlocks.size)
    ChangedBlocks.foreach(bud => {
      buf.writeShort(bud.func_180089_b)
      buf.writeInt(Block.getStateId(bud.getBlockState))
    })
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // ChunkPosCoord
    ChunkPosCoord = new ChunkCoordIntPair(buf.readInt(), buf.readInt())

    // ChangedBlock
    val sizeOf = buf.readInt()
    ChangedBlocks = (0 until sizeOf).map(i => new BlockUpdateData(buf.readShort(), Block.getStateById(buf.readInt()))).toArray
  }

  def getPos(cpc: Short): BlockPos = {
    ChunkPosCoord.getBlock(cpc >> 12 & 15, cpc & 255, cpc >> 8 & 15)
  }

  case class BlockUpdateData(chunkPosCrammed: Short, blockState: IBlockState) {

    def this(par1: Short, chunkIn: Chunk) = this(par1, chunkIn.getBlockState(MultipleBlocksChangedMessage.this.getPos(par1)))

    def func_180089_b: Short = {
      return this.chunkPosCrammed
    }

    def getBlockState: IBlockState = {
      return this.blockState
    }
  }

}

class ClientMultipleBlocksChangedMessageHandler extends IMessageHandler[MultipleBlocksChangedMessage, IMessage] {
  override def onMessage(message: MultipleBlocksChangedMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID != -1)
      Minecraft.getMinecraft.addScheduledTask(new MultipleBlocksChangedMessageTask(message, ctx))
    null
  }

  case class MultipleBlocksChangedMessageTask(Message: MultipleBlocksChangedMessage, Ctx: MessageContext) extends Runnable {
    override def run(): Unit = {
      val ship = ShipLocator.getClientShip(Message.ShipID)
      if (!FlyingShips.flyingShipPacketHandler.nullCheck(ship, "MultipleBlocksChangedMessageTask", Message.ShipID)) return

      Message.ChangedBlocks.foreach(block => ship.get.Shipworld.setBlockState(Message.getPos(block.func_180089_b), block.getBlockState, 3))
    }
  }

}
