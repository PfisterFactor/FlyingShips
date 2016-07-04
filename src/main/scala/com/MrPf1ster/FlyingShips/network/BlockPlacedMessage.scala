package com.MrPf1ster.FlyingShips.network

import com.MrPf1ster.FlyingShips.ShipWorld
import com.MrPf1ster.FlyingShips.util.ShipLocator
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.util._
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 6/30/2016.
  */
class BlockPlacedMessage(shipID:Int, pos:BlockPos, side:Int, heldItem:ItemStack, hitVec:Vec3) extends IMessage {

  def this() = this(-1, null, 0, null, null)

  var BlockPosition = pos
  var PlacedBlockDirection = side
  var HeldItem = heldItem
  var HitVec = hitVec
  var ShipID = shipID


  override def toBytes(buf: ByteBuf): Unit = {

    // BlockPosition
    buf.writeLong(BlockPosition.toLong)

    // Side
    buf.writeInt(PlacedBlockDirection)

    // Held Item
    ByteBufUtils.writeItemStack(buf,HeldItem)

    // HitVec
    buf.writeDouble(HitVec.xCoord)
    buf.writeDouble(HitVec.yCoord)
    buf.writeDouble(HitVec.zCoord)


    // ShipID
    buf.writeInt(ShipID)

  }

  override def fromBytes(buf: ByteBuf): Unit = {

    // BlockPosition
    BlockPosition = BlockPos.fromLong(buf.readLong())

    // Side
    PlacedBlockDirection = buf.readInt()

    // Held Item
    HeldItem = ByteBufUtils.readItemStack(buf)

    // HitVec
    HitVec = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())

    // ShipID
    ShipID = buf.readInt()

  }
}

class ServerBlockPlacedMessageHandler extends IMessageHandler[BlockPlacedMessage, IMessage] {
  override def onMessage(message: BlockPlacedMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID == -1) return null
    Minecraft.getMinecraft.addScheduledTask(new BlockActivatedMessageTask(message, ctx))
    null
  }

  class BlockActivatedMessageTask(message: BlockPlacedMessage, ctx: MessageContext) extends Runnable {
    val Message = message
    val Context = ctx



    // On Server
    override def run(): Unit = {

      def player = ctx.getServerHandler.playerEntity

      val ship = ShipLocator.getShip(player.worldObj,message.ShipID)


      if (ship.isEmpty)
        return


      processPacket(ctx.getServerHandler, ship.get.ShipWorld)

    }

    private def processPacket(netHandlerPlayServer: NetHandlerPlayServer, shipWorld:ShipWorld): Unit = {

      def player = netHandlerPlayServer.playerEntity
      var itemstack: ItemStack = player.inventory.getCurrentItem
      var flag: Boolean = false
      var placeResult: Boolean = true
      val blockpos: BlockPos = message.BlockPosition
      val enumfacing: EnumFacing = EnumFacing.getFront(message.PlacedBlockDirection)

      player.markPlayerActive()

      if (message.PlacedBlockDirection == 255) {
        if (itemstack == null) {
          return
        }
        val event: PlayerInteractEvent = net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(player, net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_AIR, shipWorld, new BlockPos(0, 0, 0), null)
        if (event.useItem != net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) {
          player.theItemInWorldManager.tryUseItem(player, shipWorld, itemstack)
        }
      }
      else {
        var dist: Double = player.theItemInWorldManager.getBlockReachDistance + 3
        dist *= dist
        if (shipWorld.getWorldBorder.contains(blockpos)) {
          placeResult = player.theItemInWorldManager.activateBlockOrUseItem(player, shipWorld, itemstack, blockpos, enumfacing, message.HitVec.xCoord.toFloat, message.HitVec.yCoord.toFloat, message.HitVec.zCoord.toFloat)
        }
        flag = true
      }


      // Shipworld handles updating the client
      /*
      if (flag) {
        this.playerEntity.playerNetServerHandler.sendPacket(new S23PacketBlockChange(worldserver, blockpos))
        this.playerEntity.playerNetServerHandler.sendPacket(new S23PacketBlockChange(worldserver, blockpos.offset(enumfacing)))
      }
      */

      itemstack = player.inventory.getCurrentItem

      if (itemstack != null && itemstack.stackSize == 0) {
        player.inventory.mainInventory(player.inventory.currentItem) = null
        itemstack = null
      }

      if (itemstack == null || itemstack.getMaxItemUseDuration == 0) {
        player.isChangingQuantityOnly = true
        player.inventory.mainInventory(player.inventory.currentItem) = ItemStack.copyItemStack(player.inventory.mainInventory(player.inventory.currentItem))
        var slot: Slot = player.openContainer.getSlotFromInventory(player.inventory, player.inventory.currentItem)
        var windowId: Int = player.openContainer.windowId
        if (slot == null) {
          slot = player.inventoryContainer.getSlotFromInventory(player.inventory, player.inventory.currentItem)
          windowId = player.inventoryContainer.windowId
        }
        player.openContainer.detectAndSendChanges
        player.isChangingQuantityOnly = false
        if (!ItemStack.areItemStacksEqual(player.inventory.getCurrentItem, message.HeldItem) || !placeResult) {
          netHandlerPlayServer.sendPacket(new S2FPacketSetSlot(windowId, slot.slotNumber, player.inventory.getCurrentItem))
        }
      }
    }
  }

}
