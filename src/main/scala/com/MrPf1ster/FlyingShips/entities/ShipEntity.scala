package com.MrPf1ster.FlyingShips.entities

import com.MrPf1ster.FlyingShips.ShipWorld
import com.MrPf1ster.FlyingShips.util.RotatedBB
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util._
import net.minecraft.world.World
/**
  * Created by EJ on 2/21/2016.
  */
object ShipEntity {
  var nextID: ThreadLocal[Int] = new ThreadLocal[Int]()
  nextID.set(0)
}
class ShipEntity(pos: BlockPos, world: World, blockSet: Set[BlockPos], shipBlockPos: BlockPos) extends Entity(world) {

  def this(world: World) = this(new BlockPos(0, 0, 0), world, Set[BlockPos](), new BlockPos(0, 0, 0))


  posX = shipBlockPos.getX
  posY = shipBlockPos.getY
  posZ = shipBlockPos.getZ

  var ShipBlockPos: BlockPos = shipBlockPos
  val ShipWorld: ShipWorld = new ShipWorld(world, shipBlockPos, blockSet, this)
  val ShipID = if (blockSet.nonEmpty) ShipEntity.nextID.get else -1

  var prevRotationRoll = 0.0f
  var rotationRoll = 0.0f

  if (blockSet.nonEmpty) ShipEntity.nextID.set(ShipEntity.nextID.get + 1)


  val InteractionHandler: ShipInteractionHandler = new ShipInteractionHandler
  private var boundingBox = ShipWorld.genBoundingBox()
  private var relativeBoundingBox = ShipWorld.genRelativeBoundingBox()


  override def getEntityBoundingBox = if (ShipWorld.BlockSet.nonEmpty) boundingBox else new AxisAlignedBB(0, 0, 0, 0, 0, 0)

  def getRelativeBoundingBox = relativeBoundingBox

  var RotatedBB = new RotatedBB(getEntityBoundingBox, new Vec3(ShipBlockPos.getX, ShipBlockPos.getY, ShipBlockPos.getZ), rotationYaw, rotationPitch, rotationRoll)

  def getRelativeRotated = {
    new RotatedBB(getRelativeBoundingBox, new Vec3(0, 0, 0), rotationYaw, rotationPitch, rotationRoll)
  }

  override def writeEntityToNBT(tagCompound: NBTTagCompound): Unit = {

  }

  override def readEntityFromNBT(tagCompund: NBTTagCompound): Unit = {

  }

  override def entityInit(): Unit = {
  }
  override def onUpdate() = {
    if (!ShipWorld.isValid) {
      this.setDead()
    }
    setPositionAndRotation(posX, posY, posZ, rotationYaw, rotationPitch)
    prevRotationRoll = rotationRoll
    rotationRoll = rotationRoll + 1f
  }

  override def setPosition(x: Double, y: Double, z: Double) = {
    if (boundingBox != null) {
      // Get delta...
      val deltaX = x - posX
      val deltaY = y - posY
      val deltaZ = z - posZ

      // ..and offset the bounding box
      boundingBox = boundingBox.offset(deltaX, deltaY, deltaZ)
      ShipBlockPos = ShipBlockPos.add(deltaX, deltaY, deltaZ)
      RotatedBB.setPosition(x, y, z)
    }
    // Update positions
    posX = x
    posY = y
    posZ = z

  }


  override def setPositionAndRotation(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) = {
    setPosition(x, y, z)
    prevRotationYaw = rotationYaw
    prevRotationPitch = rotationPitch
    rotationYaw = yaw % 360
    rotationPitch = pitch % 360
    RotatedBB.setRotation(rotationYaw, rotationPitch, rotationRoll)
  }


  override def canBeCollidedWith = true

  override def interactAt(player: EntityPlayer, target: Vec3) = InteractionHandler.interactionFired(player, target)




}
