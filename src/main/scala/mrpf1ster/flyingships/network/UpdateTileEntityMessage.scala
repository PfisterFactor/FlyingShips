package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import net.minecraft.tileentity.{TileEntityBanner, TileEntityFlowerPot, _}
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 7/29/2016.
  */
class UpdateTileEntityMessage(shipID: Int, blockPos: BlockPos, metadata: Int, nbt: NBTTagCompound) extends IMessage {
  def this() = this(-1, new BlockPos(-1, -1, -1), -1, null)

  def this(shipID: Int, tePacket: S35PacketUpdateTileEntity) = this(shipID, tePacket.getPos, tePacket.getTileEntityType, tePacket.getNbtCompound)

  var ShipID = shipID
  var Blockpos = blockPos
  var Metadata = metadata
  var Nbt = nbt

  override def toBytes(buf: ByteBuf): Unit = {
    // ShipID
    buf.writeInt(shipID)

    // Blockpos
    buf.writeLong(Blockpos.toLong)

    // Metadata
    buf.writeInt(metadata)

    // Nbt
    ByteBufUtils.writeTag(buf, Nbt)
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // ShipID
    ShipID = buf.readInt()

    // Blockpos
    Blockpos = BlockPos.fromLong(buf.readLong())

    // Metadata
    Metadata = buf.readInt()

    // Nbt
    Nbt = ByteBufUtils.readTag(buf)
  }

  def getPacket: S35PacketUpdateTileEntity = new S35PacketUpdateTileEntity(Blockpos, Metadata, Nbt)
}

class ClientUpdateTileEntityMessageHandler extends IMessageHandler[UpdateTileEntityMessage, IMessage] {
  override def onMessage(message: UpdateTileEntityMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID != -1)
      Minecraft.getMinecraft.addScheduledTask(new UpdateTileEntityMessageTask(message, ctx))
    null
  }

  case class UpdateTileEntityMessageTask(Message: UpdateTileEntityMessage, Ctx: MessageContext) extends Runnable {
    override def run(): Unit = {
      val ship = ShipLocator.getClientShip(Message.ShipID)

      if (!FlyingShips.flyingShipPacketHandler.nullCheck(ship, "UpdateTileEntityMessageTask", Message.ShipID)) return

      def shipWorld = ship.get.Shipworld

      if (shipWorld.isBlockLoaded(Message.Blockpos)) {
        val tileentity: TileEntity = shipWorld.getTileEntity(Message.Blockpos)
        val i: Int = Message.Metadata
        if (i == 1 && tileentity.isInstanceOf[TileEntityMobSpawner] || i == 2 && tileentity.isInstanceOf[TileEntityCommandBlock] || i == 3 && tileentity.isInstanceOf[TileEntityBeacon] || i == 4 && tileentity.isInstanceOf[TileEntitySkull] || i == 5 && tileentity.isInstanceOf[TileEntityFlowerPot] || i == 6 && tileentity.isInstanceOf[TileEntityBanner]) {
          tileentity.readFromNBT(Message.Nbt)
        }
        else {
          tileentity.onDataPacket(Ctx.getClientHandler.getNetworkManager, Message.getPacket)
        }
      }
    }
  }

}