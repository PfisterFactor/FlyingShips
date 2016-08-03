package mrpf1ster.flyingships.network

import javax.vecmath.Quat4f

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 7/30/2016.
  */
class ShipRotMessage(ship: EntityShip) extends IMessage {
  def this() = this(null)

  var ShipID = if (ship != null) ship.ShipID else -1
  var Rotation = if (ship != null) ship.getRotation else null

  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeInt(ShipID)

    // Rotation
    buf.writeFloat(Rotation.x)
    buf.writeFloat(Rotation.y)
    buf.writeFloat(Rotation.z)
    buf.writeFloat(Rotation.w)
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // ShipID
    ShipID = buf.readInt()

    // Rotation
    Rotation = new Quat4f(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat())
  }
}

class ClientShipRotMessageHandler extends IMessageHandler[ShipRotMessage, IMessage] {
  override def onMessage(message: ShipRotMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID != -1)
      Minecraft.getMinecraft.addScheduledTask(new ShipRotMessageTask(message, ctx))
    null
  }

  case class ShipRotMessageTask(message: ShipRotMessage, ctx: MessageContext) extends Runnable {
    override def run(): Unit = {
      val Ship = ShipLocator.getClientShip(message.ShipID)
      if (!FlyingShips.flyingShipPacketHandler.nullCheck(Ship, "ShipRotMessageTask", message.ShipID)) return
      Ship.get.setRotationFromServer(message.Rotation)
    }
  }

}
