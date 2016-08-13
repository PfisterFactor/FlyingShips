package mrpf1ster.flyingships.network

import javax.vecmath.Quat4f

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.entities.{EntityShip, EntityShipTracker}
import mrpf1ster.flyingships.util.UnifiedPos
import mrpf1ster.flyingships.world.{ShipWorldClient, ShipWorldServer}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.play.server.S21PacketChunkData
import net.minecraft.util.{BlockPos, MathHelper}
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

import scala.collection.mutable

/**
  * Created by EJ on 7/1/2016.
  */

// Carries info related to spawning a EntityShip on the client
// Stuff like the Position, rotation, and chunks
// Big ships might be a bit too much data however...
// Todo: Make sure big ships aren't too much data, however
class SpawnShipMessage(ship: EntityShip, player: EntityPlayerMP) extends IMessage {
  // Default constructor when the message is recreated on client
  def this() = this(null, null)

  // Message contents
  var ShipID = if (ship != null) ship.ShipID else -1
  var X: Int = if (ship != null) MathHelper.floor_double(ship.posX * 32.0D) else -1
  var Y: Int = if (ship != null) MathHelper.floor_double(ship.posY * 32.0D) else -1
  var Z: Int = if (ship != null) MathHelper.floor_double(ship.posZ * 32.0D) else -1
  var Rotation: Quat4f = if (ship != null) ship.getRotation else new Quat4f(0, 0, 0, 1f)

  // Gets all chunks on ship and extracts their data along with the coordinates
  private val chunksToSave: Map[ChunkCoordIntPair, S21PacketChunkData.Extracted] = if (ship != null) ship.Shipworld.ChunksOnShip.map(chunkCoord => (chunkCoord, S21PacketChunkData.func_179756_a(ship.Shipworld.getChunkFromChunkCoords(chunkCoord.chunkXPos, chunkCoord.chunkZPos), true, true, 65535))).toMap else Map()
  var ChunkLength: Int = chunksToSave.size
  var ChunkCoords: Array[ChunkCoordIntPair] = chunksToSave.keys.toArray
  var ChunkData: Array[S21PacketChunkData.Extracted] = chunksToSave.values.toArray
  var BlockPosLength: Int = if (ship != null) ship.Shipworld.BlocksOnShip.size else -1
  var Blockpos: Set[BlockPos] = if (ship != null) ship.Shipworld.BlocksOnShip.map(pos => pos.RelativePos).toSet else Set()

  // Contains a bunch of booleans that tell the client what chunks he should be able to render
  var ChunkWatchingArray: Array[Boolean] = if (ship != null) ship.Shipworld.ChunksOnShip.toArray.map(coord => ship.Shipworld.asInstanceOf[ShipWorldServer].PlayerManager.isPlayerWatchingChunk(player, coord)) else Array()




  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeInt(ShipID)

    // X
    buf.writeInt(X)

    // Y
    buf.writeInt(Y)

    // Z
    buf.writeInt(Z)

    // Rotation
    buf.writeFloat(Rotation.x)
    buf.writeFloat(Rotation.y)
    buf.writeFloat(Rotation.z)
    buf.writeFloat(Rotation.w)

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
    ChunkWatchingArray.foreach(buf.writeBoolean)

  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // Ship ID
    ShipID = buf.readInt()

    // X
    X = buf.readInt()

    // Y
    Y = buf.readInt()

    // Z
    Z = buf.readInt()

    // Rotation
    Rotation = new Quat4f(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat())

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

  // Does some wizardry
  // But it works
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

// Handles spawning a ship and adding it to the clientside tracker
// Exectuted only on client
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

    // The message coords are converted into ints to remove floating point precision issues
    // Or at least that's what I think is the case
    // Regardless, we convert them back to doubles and store them in these conveniently named variables corresponding to their axis
    val x = Message.X / 32.0d
    val y = Message.Y / 32.0d
    val z = Message.Z / 32.0d

    val ship = new EntityShip(new BlockPos(x, y, z), player.worldObj)
    ship.setShipID(Message.ShipID)

    ship.serverPosX = Message.X
    ship.serverPosY = Message.Y
    ship.serverPosZ = Message.Z
    ship.setPosition(x, y, z)

    // Create a new shipworld
    ship.createShipWorld()


    val shipWorld = ship.Shipworld.asInstanceOf[ShipWorldClient]

    // Sets the blocks on ship to the Message's blockpos, but converts everything to a UnifiedPos first
    shipWorld.BlocksOnShip = mutable.Set(Message.Blockpos.map(UnifiedPos(_, shipWorld.OriginPos, IsRelative = true)).toSeq: _*)
    // Adds chunks for each block to the ship
    shipWorld.BlocksOnShip.foreach(uPos => shipWorld.ChunksOnShip.add(new ChunkCoordIntPair(uPos.RelPosX >> 4, uPos.RelPosZ >> 4)))

    // Gets all the chunks currently on the ship by their coordinates
    // If the chunk doesn't exist (which on first spawn they dont), they are generated as long as ChunksOnShip contains a coordinate corresponding to what was requested
    val chunks: Array[Chunk] = Message.ChunkCoords.map(coord => shipWorld.getChunkFromChunkCoords(coord.chunkXPos, coord.chunkZPos))
    // Pairs the client chunks with their chunkdata from the server, and loads the data into the client chunks
    chunks.zip(Message.ChunkData).foreach(pair => pair._1.fillChunk(pair._2.data, pair._2.dataSize, true))

    // Updates our ChunksToRender with the relevant data from the server
    (0 until Message.ChunkLength).foreach(i => {
      if (Message.ChunkWatchingArray(i))
        shipWorld.ChunksToRender.add(Message.ChunkCoords(i))
    })

    // Tells the Shipworld about any tileentities that may have stowed away in the chunkdata
    shipWorld.BlocksOnShip.foreach(pos => {
      val te = shipWorld.getTileEntity(pos.RelativePos)
      if (te != null)
        shipWorld.loadedTileEntityList.add(te)
    })

    // Generate a bounding box based on the blocks that were just added
    ship.generateBoundingBox()
    ship.setRotationFromServer(Message.Rotation)
    ship.setInterpolatedRotation(Message.Rotation)

    // Tell the garbage collector to not eat this ship and the clientside tracker to keep it tucked away in a set
    EntityShipTracker.addShipClientSide(ship)

  }
}
