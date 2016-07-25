package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.network.ClientSpawnShipHandler.SpawnQueue
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.client.Minecraft
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

import scala.collection.mutable

/**
  * Created by EJ on 7/1/2016.
  */
class SpawnShipMessage(ship: EntityShip) extends IMessage {
  private val shipdata = if (ship != null) ship.ShipWorld.getWorldData else (Array[Byte](), Array[Byte]())
  var ShipID = if (ship != null) ship.getEntityId else -1
  var Position: BlockPos = if (ship != null) ship.getPosition else new BlockPos(-1, -1, -1)
  var BlockData: Array[Byte] = if (ship != null) shipdata._1 else Array()
  var BlockLength: Int = BlockData.length
  var TileEntityData: Array[Byte] = if (ship != null) shipdata._2 else Array()
  var TileEntityLength: Int = TileEntityData.length

  def this() = this(null)

  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeInt(ShipID)

    // Ship Position
    buf.writeLong(Position.toLong)

    // Block Data Length
    buf.writeInt(BlockLength)

    // Block Data
    buf.writeBytes(BlockData)

    // Tile Entity Data Length
    buf.writeInt(TileEntityLength)

    // Tile Entity Data
    buf.writeBytes(TileEntityData)


  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // Ship ID
    ShipID = buf.readInt()

    // Ship Position
    Position = BlockPos.fromLong(buf.readLong())

    // Block Data Length
    BlockLength = buf.readInt()

    // Block Data
    BlockData = new Array[Byte](BlockLength)
    buf.readBytes(BlockData)

    // Tile Entity Data Length
    TileEntityLength = buf.readInt()

    // Tile Entity Data
    TileEntityData = new Array[Byte](TileEntityLength)
    buf.readBytes(TileEntityData)

  }
}

object ClientSpawnShipHandler {
  type SpawnQueue = mutable.Map[Int, SpawnShipMessage]

  val spawnQueue: SpawnQueue = mutable.Map()

  // Called every time a ship is spawned. If we have a message waiting for the entity spawn packet it is handled here.
  def onShipSpawn(shipID: Int) = {
    val message = spawnQueue.get(shipID)
    if (message.isDefined) {
      Minecraft.getMinecraft.addScheduledTask(new SpawnShipMessageTask(message.get, null, spawnQueue))
      println("taken out of spawnqueue")
    }


  }
}
class ClientSpawnShipHandler extends IMessageHandler[SpawnShipMessage, IMessage] {
  override def onMessage(message: SpawnShipMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID == -1) return null
    Minecraft.getMinecraft.addScheduledTask(new SpawnShipMessageTask(message, ctx, ClientSpawnShipHandler.spawnQueue))
    null
  }

}

class SpawnShipMessageTask(message: SpawnShipMessage, ctx: MessageContext, spawnQueue: SpawnQueue) extends Runnable {
  val Message = message
  val Context = ctx
  val SpawnQueue = spawnQueue

  // On Client
  override def run(): Unit = {
    def player = Minecraft.getMinecraft.thePlayer

    val ship = ShipLocator.getShip(player.worldObj, message.ShipID)

    if (ship.isEmpty) {
      // This packet got received before the vanilla entity spawn packet. So we add it to a map and wait for the packet to be received
      if (SpawnQueue.contains(message.ShipID)) {
        println(s"SpawnQueue already contains ShipID: ${message.ShipID}! Aborting spawn!")
        SpawnQueue.remove(message.ShipID)
      }
      else {
        SpawnQueue.put(message.ShipID, message)
        println("put into spawnqueue")
      }

      return
    }


    ship.get.ShipWorld.setWorldData(message.BlockData, message.TileEntityData)

    ship.get.generateBoundingBox()

    player.worldObj.spawnEntityInWorld(ship.get)

  }
}
