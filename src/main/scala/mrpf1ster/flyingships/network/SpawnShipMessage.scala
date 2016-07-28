package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.network.ClientSpawnShipHandler.SpawnMap
import mrpf1ster.flyingships.util.{ShipLocator, UnifiedPos}
import mrpf1ster.flyingships.world.ShipWorldClient
import mrpf1ster.flyingships.world.chunk.ChunkProviderShip
import net.minecraft.client.Minecraft
import net.minecraft.network.play.server.S21PacketChunkData
import net.minecraft.util.BlockPos
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

import scala.collection.mutable

/**
  * Created by EJ on 7/1/2016.
  */
class SpawnShipMessage(ship: EntityShip) extends IMessage {

  var ShipID = if (ship != null) ship.getEntityId else -1

  private val chunksToSave: mutable.Map[ChunkCoordIntPair, S21PacketChunkData.Extracted] = if (ship != null) ship.Shipworld.getChunkProvider.asInstanceOf[ChunkProviderShip].ChunkMap.map(pair => (pair._1, S21PacketChunkData.func_179756_a(pair._2, true, true, 65535))).filter(p => p._2.dataSize > 0) else mutable.Map()
  var ChunkLength: Int = chunksToSave.size
  var ChunkCoords: Array[ChunkCoordIntPair] = chunksToSave.keys.toArray
  var ChunkData: Array[S21PacketChunkData.Extracted] = chunksToSave.values.toArray
  var BlockPosLength: Int = if (ship != null) ship.Shipworld.BlocksOnShip.size else -1
  var Blockpos: Set[BlockPos] = if (ship != null) ship.Shipworld.BlocksOnShip.map(pos => pos.RelativePos).toSet else Set()


  def this() = this(null)

  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeInt(ShipID)

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

  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // Ship ID
    ShipID = buf.readInt()

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

    // BlockPos
    Blockpos = (0 until BlockPosLength).map(i => BlockPos.fromLong(buf.readLong())).toSet


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

object ClientSpawnShipHandler {
  type SpawnMap = mutable.Map[Int, SpawnShipMessage]

  val spawnMap: SpawnMap = mutable.Map()

  // Called every time a ship is spawned. If we have a message waiting for the entity spawn packet it is handled here.
  def requestShipworld(shipID: Int) = {
    val message = spawnMap.get(shipID)
    if (message.isDefined) {
      spawnMap.remove(shipID)
      Minecraft.getMinecraft.addScheduledTask(new SpawnShipMessageTask(message.get, null, spawnMap))
    }
  }
}
class ClientSpawnShipHandler extends IMessageHandler[SpawnShipMessage, IMessage] {
  override def onMessage(message: SpawnShipMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID == -1) return null
    Minecraft.getMinecraft.addScheduledTask(new SpawnShipMessageTask(message, ctx, ClientSpawnShipHandler.spawnMap))
    null
  }

}

class SpawnShipMessageTask(message: SpawnShipMessage, ctx: MessageContext, spawnMap: SpawnMap) extends Runnable {
  val Message = message
  val Context = ctx
  val SpawnMap = spawnMap

  // On Client
  override def run(): Unit = {
    def player = Minecraft.getMinecraft.thePlayer

    val ship = ShipLocator.getShip(player.worldObj, message.ShipID)

    if (ship.isEmpty) {
      // This packet got received before the vanilla entity spawn packet. So we add it to a map and wait for the packet to be received
      if (SpawnMap.contains(message.ShipID)) {
        println(s"SpawnMap already contains ShipID: ${message.ShipID}! Aborting spawn!")
        SpawnMap.remove(message.ShipID)
      }
      else {
        SpawnMap.put(message.ShipID, message)
      }

      return
    }
    ship.get.createShipWorld()

    val shipWorld = ship.get.Shipworld.asInstanceOf[ShipWorldClient]

    val chunks: Array[Chunk] = message.ChunkCoords.map(coord => shipWorld.getChunkFromChunkCoords(coord.chunkXPos, coord.chunkZPos))
    chunks.zip(message.ChunkData).foreach(pair => pair._1.fillChunk(pair._2.data, pair._2.dataSize, true))

    shipWorld.BlocksOnShip = mutable.Set(message.Blockpos.map(UnifiedPos(_, shipWorld.OriginPos, IsRelative = true)).toSeq: _*)

    shipWorld.BlocksOnShip.foreach(pos => {
      val te = shipWorld.getTileEntity(pos.RelativePos)
      if (te != null)
        shipWorld.loadedTileEntityList.add(te)
    })

    ship.get.generateBoundingBox()

    player.worldObj.spawnEntityInWorld(ship.get)

  }
}
