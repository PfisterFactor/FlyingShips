package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 7/30/2016.
  */
class ShipVelocityMessage(shipID: Int, motionX: Double, motionY: Double, motionZ: Double) extends IMessage {
  def this() = this(-1, -1, -1, -1)

  var ShipID = shipID
  var MotionX: Int = (motionX * 8000.0D).toInt
  var MotionY: Int = (motionY * 8000.0D).toInt
  var MotionZ: Int = (motionZ * 8000.0D).toInt

  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeInt(ShipID)

    // MotionX
    buf.writeInt(MotionX)

    // MotionY
    buf.writeInt(MotionY)

    // MotionZ
    buf.writeInt(MotionZ)

  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // ShipID
    ShipID = buf.readInt()

    // MotionX
    MotionX = buf.readInt()

    // MotionY
    MotionY = buf.readInt()

    // MotionZ
    MotionZ = buf.readInt()

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
      val motionX = Message.MotionX / 8000.0D
      val motionY = Message.MotionY / 8000.0D
      val motionZ = Message.MotionZ / 8000.0D
      Ship.get.setVelocity(motionX, motionY, motionZ)
    }
  }

}
