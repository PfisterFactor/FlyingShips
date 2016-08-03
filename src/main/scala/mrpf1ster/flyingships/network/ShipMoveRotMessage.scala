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
class ShipMoveRotMessage(shipID: Int, posX: Int, posY: Int, posZ: Int, rotation: Quat4f, doTeleport: Boolean) extends IMessage {
  def this() = this(-1, -1.toByte, -1.toByte, -1.toByte, null, false)

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
    buf.writeInt(PosX)

    // PosY
    buf.writeInt(PosY)

    // PosZ
    buf.writeInt(PosZ)

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
    PosX = buf.readInt()

    // PosY
    PosY = buf.readInt()

    // PosZ
    PosZ = buf.readInt()

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
        Ship.get.serverPosX = message.PosX
        Ship.get.serverPosY = message.PosY
        Ship.get.serverPosZ = message.PosZ
      }
      else {
        Ship.get.serverPosX += message.PosX
        Ship.get.serverPosY += message.PosY
        Ship.get.serverPosZ += message.PosZ
      }

      val x = Ship.get.serverPosX / 32.0d
      val y = Ship.get.serverPosY / 32.0d
      val z = Ship.get.serverPosZ / 32.0d

      Ship.get.setPosition(x, y, z)
      Ship.get.setRotationFromServer(message.Rotation)
    }
  }

}
