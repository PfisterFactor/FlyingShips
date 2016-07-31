package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 7/30/2016.
  */
class ShipRelMoveMessage(shipID: Int, posX: Double, posY: Double, posZ: Double, doTeleport: Boolean) extends IMessage {
  def this() = this(-1, -1, -1, -1, false)

  var ShipID = shipID
  var PosX = posX
  var PosY = posY
  var PosZ = posZ

  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeInt(ShipID)

    // PosX
    buf.writeDouble(PosX)

    // PosY
    buf.writeDouble(PosY)

    // PosZ
    buf.writeDouble(PosZ)

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

  }
}

class ClientShipRelMoveMessageHandler extends IMessageHandler[ShipRelMoveMessage, IMessage] {
  override def onMessage(message: ShipRelMoveMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID != -1)
      Minecraft.getMinecraft.addScheduledTask(new ShipRelMoveMessageTask(message, ctx))
    null
  }

  case class ShipRelMoveMessageTask(Message: ShipRelMoveMessage, Ctx: MessageContext) extends Runnable {
    override def run(): Unit = {
      val Ship = ShipLocator.getClientShip(Message.ShipID)
      if (!FlyingShips.flyingShipPacketHandler.nullCheck(Ship, "ShipRelMoveMessageTask", Message.ShipID)) return

      Ship.get.serverPosX += Message.PosX.toInt
      Ship.get.serverPosY += Message.PosY.toInt
      Ship.get.serverPosZ += Message.PosZ.toInt

      Ship.get.setPosition(Ship.get.serverPosX, Ship.get.serverPosY, Ship.get.serverPosZ)


    }
  }

}
