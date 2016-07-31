package mrpf1ster.flyingships.network

import javax.vecmath.Quat4f

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 7/30/2016.
  */
class ShipMoveRotMessage(shipID: Int, posX: Double, posY: Double, posZ: Double, rotation: Quat4f, doTeleport: Boolean) extends IMessage {
  def this() = this(-1, -1, -1, -1, null, false)

  var ShipID = shipID
  var PosX = posX
  var PosY = posY
  var PosZ = posZ
  var Rotation = rotation
  var DoTeleport = doTeleport

  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeInt(ShipID)

    // PosX
    buf.writeDouble(PosX)

    // PosY
    buf.writeDouble(PosY)

    // PosZ
    buf.writeDouble(PosZ)

    // Rotation
    buf.writeFloat(Rotation.x)
    buf.writeFloat(Rotation.y)
    buf.writeFloat(Rotation.z)
    buf.writeFloat(Rotation.w)

    // DoTeleport
    buf.writeBoolean(DoTeleport)
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // ShipID
    ShipID = buf.readInt()

    // PosX
    PosX = buf.readDouble()

    // PosY
    PosY = buf.readDouble()

    // PosZ
    PosZ = buf.readDouble()

    // Rotation
    Rotation = new Quat4f(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat())

    // DoTeleport
    DoTeleport = buf.readBoolean()

  }
}

class ClientShipMoveRotMessageHandler extends IMessageHandler[ShipMoveRotMessage, IMessage] {
  override def onMessage(message: ShipMoveRotMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID != -1)
      Minecraft.getMinecraft.addScheduledTask(new ShipMoveRotMessageTask(message, ctx))
    null
  }

  case class ShipMoveRotMessageTask(message: ShipMoveRotMessage, ctx: MessageContext) extends Runnable {
    override def run(): Unit = {
      val Ship = ShipLocator.getClientShip(message.ShipID)
      if (!FlyingShips.flyingShipPacketHandler.nullCheck(Ship, "ShipMoveRotMessageTask", message.ShipID)) return

      if (message.DoTeleport) {
        Ship.get.serverPosX = message.PosX.toInt
        Ship.get.serverPosY = message.PosY.toInt
        Ship.get.serverPosZ = message.PosZ.toInt

        Ship.get.setPosition(message.PosX, message.PosY, message.PosZ)
        Ship.get.setRotation(message.Rotation)
      }
      else {
        Ship.get.serverPosX += message.PosX.toInt
        Ship.get.serverPosY += message.PosY.toInt
        Ship.get.serverPosZ += message.PosZ.toInt
        Ship.get.setPosition(Ship.get.serverPosX, Ship.get.serverPosY, Ship.get.serverPosZ)
        Ship.get.setRotation(message.Rotation)
      }
    }
  }

}
