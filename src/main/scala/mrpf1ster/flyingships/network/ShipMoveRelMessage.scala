package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 7/30/2016.
  */
class ShipRelMoveMessage(shipID: Int, posX: Byte, posY: Byte, posZ: Byte) extends IMessage {
  def this() = this(-1, -1.toByte, -1.toByte, -1.toByte)

  var ShipID: Int = shipID
  var PosX: Byte = posX
  var PosY: Byte = posY
  var PosZ: Byte = posZ

  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeInt(ShipID)

    // PosX
    buf.writeByte(PosX)

    // PosY
    buf.writeByte(PosY)

    // PosZ
    buf.writeByte(PosZ)

  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // ShipID
    ShipID = buf.readInt()

    // PosX
    PosX = buf.readByte()

    // PosY
    PosY = buf.readByte()

    // PosZ
    PosZ = buf.readByte()

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

      Ship.get.serverPosX += Message.PosX
      Ship.get.serverPosY += Message.PosY
      Ship.get.serverPosZ += Message.PosZ

      val x = Ship.get.serverPosX / 32.0d
      val y = Ship.get.serverPosY / 32.0d
      val z = Ship.get.serverPosZ / 32.0d

      Ship.get.setPosition(x, y, z)


    }
  }

}
