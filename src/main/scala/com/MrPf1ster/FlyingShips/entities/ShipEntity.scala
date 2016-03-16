package com.MrPf1ster.FlyingShips.entities

import com.MrPf1ster.FlyingShips.ShipWorld
import com.MrPf1ster.FlyingShips.blocks.ShipCreatorBlock
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

  val ShipWorld: ShipWorld = new ShipWorld(world, shipBlockPos, blockSet, this)


  posX = shipBlockPos.getX
  posY = shipBlockPos.getY
  posZ = shipBlockPos.getZ
  val ShipID = if (blockSet.nonEmpty) ShipEntity.nextID.get else -1
  val InteractionHandler: ShipInteractionHandler = new ShipInteractionHandler
  var ShipBlockPos: BlockPos = shipBlockPos
  var prevRotationRoll = 0.0f
  var rotationRoll = 0.0f

  if (blockSet.nonEmpty) ShipEntity.nextID.set(ShipEntity.nextID.get + 1)
  var RotatedBB = new RotatedBB(getEntityBoundingBox, new Vec3(ShipBlockPos.getX, ShipBlockPos.getY, ShipBlockPos.getZ), rotationOffset(shipDirection), rotationYaw, rotationPitch, rotationRoll)
  private var boundingBox = ShipWorld.genBoundingBox()
  private var relativeBoundingBox = ShipWorld.genRelativeBoundingBox()

  def this(world: World) = this(new BlockPos(0, 0, 0), world, Set[BlockPos](), new BlockPos(0, 0, 0))

  override def getEntityBoundingBox = if (ShipWorld.isValid) boundingBox else new AxisAlignedBB(0, 0, 0, 0, 0, 0)

  def getRelativeRotated = {
    new RotatedBB(getRelativeBoundingBox, new Vec3(0.5, 0.5, 0.5), rotationOffset(shipDirection), rotationYaw, rotationPitch, rotationRoll)
  }

  def getRelativeBoundingBox = relativeBoundingBox

  def shipDirection = if (ShipWorld.isValid) ShipWorld.ShipBlock.getValue(ShipCreatorBlock.FACING).getDirectionVec else null

  def rotationOffset(direction: Vec3i): Float = {
    if (direction == null) 0f
    else if (direction.getX == 1) 0f
    else if (direction.getX == -1) 180f
    else if (direction.getZ == 1) 90f
    else if (direction.getZ == -1) 270f
    else 0f

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
    setPositionAndRotation(posX, posY, posZ, rotationYaw + 1f, rotationPitch + 1f)
    prevRotationRoll = rotationRoll
    rotationRoll = 0
  }

  override def setPositionAndRotation(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) = {
    setPosition(x, y, z)
    prevRotationYaw = rotationYaw
    prevRotationPitch = rotationPitch

    rotationYaw = yaw % 360
    rotationPitch = pitch % 360
    RotatedBB.setRotation(rotationYaw, rotationPitch, rotationRoll)
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

  override def canBeCollidedWith = true

  override def interactAt(player: EntityPlayer, target: Vec3) = InteractionHandler.interactionFired(player, target)




}
