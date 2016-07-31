package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.client.Minecraft
import net.minecraft.network.play.server.S21PacketChunkData
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 7/29/2016.
  */
class ChunkDataMessage(shipID: Int, chunk: Chunk, par2: Boolean, par3: Int) extends IMessage {
  def this() = this(-1, null, false, -1)

  var ShipID = shipID
  var Par2 = par2
  var ChunkData = if (chunk != null) S21PacketChunkData.func_179756_a(chunk, Par2, true, par3) else null
  var ChunkX = if (chunk != null) chunk.xPosition else -1
  var ChunkZ = if (chunk != null) chunk.zPosition else -1

  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeInt(ShipID)

    // Par2
    buf.writeBoolean(Par2)

    // ChunkX
    buf.writeInt(ChunkX)

    // ChunkZ
    buf.writeInt(ChunkZ)

    // ChunkDataSize
    buf.writeShort(ChunkData.dataSize & 65535)

    // ChunkData
    buf.writeBytes(ChunkData.data)
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // Ship ID
    ShipID = buf.readInt()

    // Par2
    Par2 = buf.readBoolean()

    // ChunkX
    ChunkX = buf.readInt()

    // ChunkZ
    ChunkZ = buf.readInt()

    // ChunkDataSize
    ChunkData = new S21PacketChunkData.Extracted
    ChunkData.dataSize = buf.readShort() & 65535

    // ChunkData
    ChunkData.data = new Array[Byte](ChunkData.dataSize)
    buf.readBytes(ChunkData.data)

  }

}

class ClientChunkDataMessageHandler extends IMessageHandler[ChunkDataMessage, IMessage] {
  override def onMessage(message: ChunkDataMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID != -1)
      Minecraft.getMinecraft.addScheduledTask(new ChunkDataMessageTask(message, ctx))
    null
  }

  class ChunkDataMessageTask(message: ChunkDataMessage, ctx: MessageContext) extends Runnable {
    val Message = message
    val Context = ctx

    // On Client
    override def run(): Unit = {
      val ship = ShipLocator.getClientShip(message.ShipID)
      if (!FlyingShips.flyingShipPacketHandler.nullCheck(ship, "ChunkDataMessageTask", message.ShipID)) return

      val chunk = ship.get.Shipworld.getChunkFromChunkCoords(message.ChunkX, message.ChunkZ)
      chunk.fillChunk(message.ChunkData.data, message.ChunkData.dataSize, message.Par2)
    }

  }

}
