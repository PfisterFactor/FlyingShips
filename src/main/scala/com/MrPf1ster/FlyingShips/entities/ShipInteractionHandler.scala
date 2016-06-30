package com.MrPf1ster.FlyingShips.entities

import javax.vecmath.Quat4f

import com.MrPf1ster.FlyingShips.util.VectorUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.{BlockPos, Vec3}

import scala.util.Try

/**
  * Created by EJ on 3/6/2016.
  */


case class ShipInteractionHandler(Ship:ShipEntity) {

  // This method gets the block the mouse is over on the passed ship entity
  // It rotates the look and ray vector against the ship's current rotation so we can use Minecraft's built in world block ray-trace
  def getBlockPlayerIsLookingAt(partialTicks: Float): Option[BlockPos] = {

    // Gets the object our mouse is over
    def objectMouseOver = Minecraft.getMinecraft.objectMouseOver

    // If the object isn't an entity or its not the ship entity currently being rendered, exit
    if (objectMouseOver.typeOfHit != MovingObjectType.ENTITY || !objectMouseOver.entityHit.isEntityEqual(Ship)) return None

    // The dude
    def player = Minecraft.getMinecraft.thePlayer

    // Gets the opposite rotation of our entity
    val inversedRot: Quat4f = Ship.Rotation.clone().asInstanceOf[Quat4f] // clone because javax is very mutatish
    inversedRot.inverse()


    // Gets the player's reach distance
    val blockReachDistance = Ship.InteractionHandler.getPlayerReachDistance(player)


    // The player's eye position, relative to the ship block's center
    val relativeEyePos: Vec3 = player.getPositionEyes(partialTicks)
      .subtract(Ship.getPositionVector.addVector(0.5,0.5,0.5))

    // The player's look vector, i.e. where his camera is pointing
    val lookVector: Vec3 = player.getLook(partialTicks)

    // The ray we use for block ray-tracing, just the relative eye position plus the farthest the player can interact with blocks in a direction
    val ray: Vec3 = relativeEyePos.addVector(lookVector.xCoord * blockReachDistance, lookVector.yCoord * blockReachDistance, lookVector.zCoord * blockReachDistance)


    // The eye position and ray, rotated by the ship block's center, to the opposite of the ships current rotation
    val rotatedRelativeEyePos = VectorUtils.rotatePointByQuaternion(relativeEyePos,inversedRot).addVector(0.5,0.5,0.5)
    val rotatedRay = VectorUtils.rotatePointByQuaternion(ray,inversedRot).addVector(0.5,0.5,0.5)

    // The result of the ray-trace on the ship world, wrapped in an Try error catch
    val blockPos = Try(Ship.ShipWorld.rayTraceBlocks(rotatedRelativeEyePos, rotatedRay).getBlockPos)

    // Convert the try to an option (None if error, Some[BlockPos] otherwise)
    blockPos.toOption

  }

  // Unimplemented
  def interactionFired(player: EntityPlayer, vec3: Vec3): Boolean = {
    println(vec3)
    true
  }

  // Code adapted from https://bitbucket.org/cuchaz/mod-shared/
  // Thank you Cuchaz!
  // Gets the reach distance of the player
  def getPlayerReachDistance(player: EntityPlayer): Double = player match {
    case EntityPlayerMP => player.asInstanceOf[EntityPlayerMP].theItemInWorldManager.getBlockReachDistance
    case AbstractClientPlayer => Minecraft.getMinecraft.playerController.getBlockReachDistance()
    case _ => 0

  }
}
