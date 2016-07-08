package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.entities.EntityShip
import net.minecraft.client.Minecraft
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 7/1/2016.
  */
class SpawnShipMessage(ship:EntityShip) extends IMessage {
  def this() = this(null)

  var ShipID = if (ship != null) ship.getEntityId else -1
  var Position: BlockPos = if (ship != null) ship.getPosition else new BlockPos(-1,-1,-1)
  private val shipdata = if (ship != null) ship.ShipWorld.getWorldData else (Array[Byte](),Array[Byte]())
  var BlockData: Array[Byte] = if (ship != null) shipdata._1 else Array()
  var BlockLength: Int = BlockData.length
  var TileEntityData: Array[Byte] = if (ship != null) shipdata._2 else Array()
  var TileEntityLength:Int = TileEntityData.length


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

class ClientSpawnShipHandler extends IMessageHandler[SpawnShipMessage, IMessage] {
  override def onMessage(message: SpawnShipMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID == -1) return null
    Minecraft.getMinecraft.addScheduledTask(new SpawnShipMessageTask(message, ctx))
    null
  }

  class SpawnShipMessageTask(message: SpawnShipMessage, ctx: MessageContext) extends Runnable {
    val Message = message
    val Context = ctx

    // On Client
    override def run(): Unit = {

      def player = Minecraft.getMinecraft.thePlayer

      val ship = new EntityShip(message.Position,player.worldObj,Set())
      ship.setEntityId(message.ShipID)
      ship.setLocationAndAngles(message.Position.getX(),message.Position.getY,message.Position.getZ,0,0)

      ship.ShipWorld.setWorldData(message.BlockData,message.TileEntityData)

      ship.generateBoundingBox()

      player.worldObj.spawnEntityInWorld(ship)

    }
  }
}
