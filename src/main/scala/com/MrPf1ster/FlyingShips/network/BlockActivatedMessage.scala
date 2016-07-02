package com.MrPf1ster.FlyingShips.network

import java.util.UUID

import com.MrPf1ster.FlyingShips.entities.EntityShip
import com.MrPf1ster.FlyingShips.util.ShipLocator
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{BlockPos, EnumFacing, MovingObjectPosition, Vec3}
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 6/30/2016.
  */
class BlockActivatedMessage(ship: EntityShip, player: EntityPlayer, movingObjectPosition: MovingObjectPosition) extends IMessage {

  def this() = this(null, null, null)

  var BlockPosition = if (movingObjectPosition != null) movingObjectPosition.getBlockPos else new BlockPos(0, 0, 0)
  var PlayerUUID: UUID = if (player != null) player.getGameProfile.getId else new UUID(0, 0)
  var HitVec = if (movingObjectPosition != null) movingObjectPosition.hitVec else new Vec3(-1, -1, -1)
  var HitSide = if (movingObjectPosition != null) movingObjectPosition.sideHit else EnumFacing.UP
  var ShipID = if (ship != null) ship.getEntityId else -1


  override def toBytes(buf: ByteBuf): Unit = {
    // BlockPosition
    buf.writeLong(BlockPosition.toLong)

    // PlayerUUID
    buf.writeLong(PlayerUUID.getLeastSignificantBits)
    buf.writeLong(PlayerUUID.getMostSignificantBits)

    // HitVec
    buf.writeDouble(HitVec.xCoord)
    buf.writeDouble(HitVec.yCoord)
    buf.writeDouble(HitVec.zCoord)

    // HitSide
    buf.writeInt(HitSide.getIndex)

    // ShipID
    buf.writeInt(ShipID)

  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // BlockPosition
    BlockPosition = BlockPos.fromLong(buf.readLong())

    // PlayerUUID
    PlayerUUID = new UUID(buf.readLong(), buf.readLong())

    // HitVec
    HitVec = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())

    // HitSide
    HitSide = EnumFacing.VALUES(buf.readInt())

    // ShipID
    ShipID = buf.readInt()

  }
}

class ServerBlockActivatedMessageHandler extends IMessageHandler[BlockActivatedMessage, IMessage] {
  override def onMessage(message: BlockActivatedMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID == -1) return null
    Minecraft.getMinecraft.addScheduledTask(new BlockActivatedMessageTask(message, ctx))
    null
  }

  class BlockActivatedMessageTask(message: BlockActivatedMessage, ctx: MessageContext) extends Runnable {
    val Message = message
    val Context = ctx

    // On Server
    override def run(): Unit = {



      val player = ctx.getServerHandler.playerEntity
      val ship = ShipLocator.getShip(player.worldObj,message.ShipID)

      if (ship.isEmpty)
        return

      def block = message.BlockPosition
      def hitVec = message.HitVec
      def side = message.HitSide

      val blockstate = ship.get.ShipWorld.getBlockState(block)

      blockstate.getBlock.onBlockActivated(ship.get.ShipWorld, block, blockstate, player, side, hitVec.xCoord.toFloat, hitVec.yCoord.toFloat, hitVec.zCoord.toFloat)


    }
  }

}
