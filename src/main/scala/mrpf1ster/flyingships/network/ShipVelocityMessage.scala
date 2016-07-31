package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 7/30/2016.
  */
class ShipVelocityMessage(ship: EntityShip) extends IMessage {
  def this() = this(null)

  var ShipID = if (ship != null) ship.ShipID else -1
  var MotionX: Double = if (ship != null) ship.motionX else -1
  var MotionY: Double = if (ship != null) ship.motionY else -1
  var MotionZ: Double = if (ship != null) ship.motionZ else -1

  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeDouble(ShipID)

    // MotionX
    buf.writeDouble(MotionX)

    // MotionY
    buf.writeDouble(MotionY)

    // MotionZ
    buf.writeDouble(MotionZ)

  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // ShipID
    ShipID = buf.readInt()

    // MotionX
    MotionX = buf.readDouble()

    // MotionY
    MotionY = buf.readDouble()

    // MotionZ
    MotionZ = buf.readDouble()

  }
}

class ClientShipVelocityMessageHandler extends IMessageHandler[ShipVelocityMessage, IMessage] {
  override def onMessage(message: ShipVelocityMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID != -1)
      Minecraft.getMinecraft.addScheduledTask(new ShipVelocityMessageTask(message, ctx))
    null
  }

  case class ShipVelocityMessageTask(Message: ShipVelocityMessage, Ctx: MessageContext) extends Runnable {
    // On Client
    override def run(): Unit = {
      val Ship = ShipLocator.getClientShip(Message.ShipID)
      if (!FlyingShips.flyingShipPacketHandler.nullCheck(Ship, "ShipVelocityMessageTask", Message.ShipID)) return
      Ship.get.setVelocity(Message.MotionX, Message.MotionY, Message.MotionZ)
    }
  }

}
