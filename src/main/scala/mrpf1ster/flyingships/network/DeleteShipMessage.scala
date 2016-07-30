package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.entities.EntityShipTracker
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 7/30/2016.
  */
class DeleteShipMessage(shipID: Int) extends IMessage {
  def this() = this(-1)

  var ShipID = shipID

  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeInt(ShipID)
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // ShipID
    ShipID = buf.readInt()
  }
}

class ClientDeleteShipMessageHandler extends IMessageHandler[DeleteShipMessage, IMessage] {
  override def onMessage(message: DeleteShipMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID != -1)
      Minecraft.getMinecraft.addScheduledTask(new DeleteShipMessageTask(message, ctx))
    null
  }

  case class DeleteShipMessageTask(Message: DeleteShipMessage, Ctx: MessageContext) extends Runnable {
    override def run(): Unit = {
      val ship = ShipLocator.getShip(Message.ShipID)
      if (ship.isEmpty)
        println(s"DeleteShipMessageTask: Could not find Ship ID ${Message.ShipID}, cannot delete it!")
      else
        EntityShipTracker.removeShipClientSide(ship.get)
    }
  }

}
