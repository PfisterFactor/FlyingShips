package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.entities.{EntityShip, EntityShipTracker}
import mrpf1ster.flyingships.util.UnifiedPos
import mrpf1ster.flyingships.world.{ShipWorldClient, ShipWorldServer}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.play.server.S21PacketChunkData
import net.minecraft.util.BlockPos
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

import scala.collection.mutable

/**
  * Created by EJ on 7/1/2016.
  */
class SpawnShipMessage(ship: EntityShip, player: EntityPlayerMP) extends IMessage {
  def this() = this(null, null)

  var ShipID = if (ship != null) ship.ShipID else -1
  var X: Double = if (ship != null) ship.posX else -1
  var Y: Double = if (ship != null) ship.posY else -1
  var Z: Double = if (ship != null) ship.posZ else -1

  private val chunksToSave: Map[ChunkCoordIntPair, S21PacketChunkData.Extracted] = if (ship != null) ship.Shipworld.ChunksOnShip.map(chunkCoord => (chunkCoord, S21PacketChunkData.func_179756_a(ship.Shipworld.getChunkFromChunkCoords(chunkCoord.chunkXPos, chunkCoord.chunkZPos), true, true, 65535))).toMap else Map()
  var ChunkLength: Int = chunksToSave.size
  var ChunkCoords: Array[ChunkCoordIntPair] = chunksToSave.keys.toArray
  var ChunkData: Array[S21PacketChunkData.Extracted] = chunksToSave.values.toArray
  var BlockPosLength: Int = if (ship != null) ship.Shipworld.BlocksOnShip.size else -1
  var Blockpos: Set[BlockPos] = if (ship != null) ship.Shipworld.BlocksOnShip.map(pos => pos.RelativePos).toSet else Set()
  var ChunkWatchingArray: Array[Boolean] = if (ship != null) ship.Shipworld.ChunksOnShip.toArray.map(coord => ship.Shipworld.asInstanceOf[ShipWorldServer].PlayerManager.isPlayerWatchingChunk(player, coord)) else Array()




  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeInt(ShipID)

    // X
    buf.writeDouble(X)

    // Y
    buf.writeDouble(Y)

    // Z
    buf.writeDouble(Z)

    // Chunk Length
    buf.writeInt(ChunkLength)

    // Chunk Coords
    ChunkCoords.foreach(pair => {
      buf.writeInt(pair.chunkXPos); buf.writeInt(pair.chunkZPos)
    })

    // Chunk Data
    ChunkData.foreach(data => {
      buf.writeShort(data.dataSize & 65535)
      buf.writeBytes(data.data)
    })

    // BlockPosLength
    buf.writeInt(BlockPosLength)

    // Blockpos
    Blockpos.foreach(pos => buf.writeLong(pos.toLong))

    // ChunkWatchingArray
    ChunkWatchingArray.foreach(buf.writeBoolean(_))

  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // Ship ID
    ShipID = buf.readInt()

    // X
    X = buf.readDouble()

    // Y
    Y = buf.readDouble()

    // Z
    Z = buf.readDouble()

    // Chunk Length
    ChunkLength = buf.readInt()

    // Chunk Coords
    ChunkCoords = (0 until ChunkLength).map(x => new ChunkCoordIntPair(buf.readInt(), buf.readInt())).toArray

    // Chunk Data
    ChunkData = Array.fill(ChunkLength)(new S21PacketChunkData.Extracted())
    ChunkData.foreach(data => {
      data.dataSize = buf.readShort() & 65535
      data.data = new Array[Byte](func_180737_a(Integer.bitCount(data.dataSize), par2 = true, par3 = true))
      buf.readBytes(data.data)
    })

    // BlockPosLength
    BlockPosLength = buf.readInt()

    // Blockpos
    Blockpos = (0 until BlockPosLength).map(i => BlockPos.fromLong(buf.readLong())).toSet

    // ChunkWatchingArray
    ChunkWatchingArray = (0 until ChunkLength).map(x => buf.readBoolean()).toArray


  }

  protected def func_180737_a(par1: Int, par2: Boolean, par3: Boolean): Int = {
    val i: Int = par1 * 2 * 16 * 16 * 16
    val j: Int = par1 * 16 * 16 * 16 / 2
    val k: Int = if (par2) par1 * 16 * 16 * 16 / 2
    else 0
    val l: Int = if (par3) 256
    else 0
    i + j + k + l
  }
}

class ClientSpawnShipMessageHandler extends IMessageHandler[SpawnShipMessage, IMessage] {
  override def onMessage(Message: SpawnShipMessage, ctx: MessageContext): IMessage = {
    if (Message.ShipID != -1)
      Minecraft.getMinecraft.addScheduledTask(new SpawnShipMessageTask(Message, ctx))
    null
  }

}

case class SpawnShipMessageTask(Message: SpawnShipMessage, Ctx: MessageContext) extends Runnable {

  // On Client
  override def run(): Unit = {
    def player = Minecraft.getMinecraft.thePlayer

    val ship = new EntityShip(new BlockPos(Message.X, Message.Y, Message.Z), player.worldObj)
    ship.setShipID(Message.ShipID)
    ship.setPosition(Message.X, Message.Y, Message.Z)
    ship.serverPosX = Message.X.toInt
    ship.serverPosY = Message.Y.toInt
    ship.serverPosZ = Message.Z.toInt
    ship.createShipWorld()


    val shipWorld = ship.Shipworld.asInstanceOf[ShipWorldClient]

    shipWorld.BlocksOnShip = mutable.Set(Message.Blockpos.map(UnifiedPos(_, shipWorld.OriginPos, IsRelative = true)).toSeq: _*)
    shipWorld.BlocksOnShip.foreach(uPos => shipWorld.ChunksOnShip.add(new ChunkCoordIntPair(uPos.RelPosX >> 4, uPos.RelPosZ >> 4)))

    val chunks: Array[Chunk] = Message.ChunkCoords.map(coord => shipWorld.getChunkFromChunkCoords(coord.chunkXPos, coord.chunkZPos))
    chunks.zip(Message.ChunkData).foreach(pair => pair._1.fillChunk(pair._2.data, pair._2.dataSize, true))

    (0 until Message.ChunkLength).foreach(i => {
      if (Message.ChunkWatchingArray(i))
        shipWorld.ChunksToRender.add(Message.ChunkCoords(i))
    })


    shipWorld.BlocksOnShip.foreach(pos => {
      val te = shipWorld.getTileEntity(pos.RelativePos)
      if (te != null)
        shipWorld.loadedTileEntityList.add(te)
    })

    ship.generateBoundingBox()
    EntityShipTracker.addShipClientSide(ship)

  }
}
