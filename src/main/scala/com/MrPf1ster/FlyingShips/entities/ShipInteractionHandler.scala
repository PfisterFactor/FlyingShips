package com.MrPf1ster.FlyingShips.entities

import com.MrPf1ster.FlyingShips.network.BlockActivatedMessage
import com.MrPf1ster.FlyingShips.{FlyingShips, ShipWorld}
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.item.ItemBlock
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.{BlockPos, EnumFacing, MovingObjectPosition, Vec3}

/**
  * Created by EJ on 3/6/2016.
  */


case class ShipInteractionHandler(ShipWorld: ShipWorld) {

  // Gets the block the mouse is over on the passed ship entity
  def getBlockPlayerIsLookingAt(partialTicks: Float): Option[MovingObjectPosition] = {

    // Gets the object our mouse is over
    def objectMouseOver = Minecraft.getMinecraft.objectMouseOver

    // If the object isn't an entity or its not the ship entity currently being rendered, exit
    if (objectMouseOver.typeOfHit != MovingObjectType.ENTITY || !objectMouseOver.entityHit.isEntityEqual(ShipWorld.Ship)) return None


    // The dude
    def player = Minecraft.getMinecraft.thePlayer


    // Gets the player's reach distance
    val blockReachDistance = getPlayerReachDistance(player)


    // The player's eye position, relative to the ship block's center
    val eyePos: Vec3 = player.getPositionEyes(partialTicks)


    // The player's look vector, i.e. where his camera is pointing
    val lookVector: Vec3 = player.getLook(partialTicks)


    // The ray we use for block ray-tracing, just the relative eye position plus the farthest the player can interact with blocks in a direction
    val ray: Vec3 = eyePos.addVector(lookVector.xCoord * blockReachDistance, lookVector.yCoord * blockReachDistance, lookVector.zCoord * blockReachDistance)


    ShipWorld.rotatedRayTrace(eyePos, ray)

  }


  // Unimplemented
  def interactionFired(player: EntityPlayer): Boolean = {
    val hitInfo = getBlockPlayerIsLookingAt(1.0f)

    if (hitInfo.isEmpty) return false

    def pos = hitInfo.get.getBlockPos
    def hitVec = hitInfo.get.hitVec
    def side = hitInfo.get.sideHit

    val message = new BlockActivatedMessage(ShipWorld.Ship,player,hitInfo.get)
    FlyingShips.flyingShipPacketHandler.INSTANCE.sendToServer(message)

    val didRightClick = simulateRightClick(player,pos,hitVec,side)
    if (didRightClick)
      player.swingItem()

    return didRightClick
  }

  // Code adapted from https://bitbucket.org/cuchaz/mod-shared/
  // Thank you Cuchaz!
  // Gets the reach distance of the player
  def getPlayerReachDistance(player: EntityPlayer): Double = player match {
    case playerMP: EntityPlayerMP => playerMP.theItemInWorldManager.getBlockReachDistance
    case _: AbstractClientPlayer => Minecraft.getMinecraft.playerController.getBlockReachDistance()
    case _ => 0

  }

  // This is pretty much ripped right from PlayerControllerMP onPlayerRightClick
   def simulateRightClick(player:EntityPlayer,pos:BlockPos,hitVec:Vec3,side:EnumFacing): Boolean = {

    def hitVecX = hitVec.xCoord.toFloat
    def hitVecY = hitVec.yCoord.toFloat
    def hitVecZ = hitVec.zCoord.toFloat

    def heldStack = player.getHeldItem

    val blockState = ShipWorld.getBlockState(pos)

    if (heldStack != null &&
      heldStack.getItem != null &&
      heldStack.getItem.onItemUseFirst(heldStack,player,ShipWorld,pos,side,hitVecX,hitVecY,hitVecZ)) {
      return true
    }

    var flag = false

    if ((!player.isSneaking || player.getHeldItem == null || player.getHeldItem.getItem.doesSneakBypassUse(ShipWorld, pos, player))) {
      flag = blockState.getBlock.onBlockActivated(ShipWorld, pos, blockState, player, side, hitVec.xCoord.toFloat, hitVec.yCoord.toFloat, hitVec.zCoord.toFloat)
    }

    if (!flag && heldStack != null && heldStack.getItem.isInstanceOf[ItemBlock]) {

      val itemblock: ItemBlock = heldStack.getItem.asInstanceOf[ItemBlock]

      if (!itemblock.canPlaceBlockOnSide(ShipWorld, pos, side, player, heldStack))
        return false

    }

    if (!flag && !player.isSpectator) {
      if (heldStack == null) {
        return false
      }
      else if (player.capabilities.isCreativeMode) {
        val meta: Int = heldStack.getMetadata
        val stackSize: Int = heldStack.stackSize
        val flag1: Boolean = heldStack.onItemUse(player, ShipWorld, pos, side, hitVecX, hitVecY, hitVecZ)
        heldStack.setItemDamage(meta)
        heldStack.stackSize = stackSize
        return flag1
      }
      else {
        if (!heldStack.onItemUse(player, ShipWorld, pos, side, hitVecX, hitVecY, hitVecZ))
          return false

        if (heldStack.stackSize <= 0)
          net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, heldStack)

        return true
      }
    }
    else {
      return true
    }
  }
}
