package com.MrPf1ster.FlyingShips.entities

import javax.vecmath.{Matrix3f, Matrix4f, Quat4f}

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

  val ShipID = if (blockSet.nonEmpty) ShipEntity.nextID.get else -1
  val InteractionHandler: ShipInteractionHandler = new ShipInteractionHandler
  var ShipBlockPos: BlockPos = shipBlockPos
  var prevRotationRoll = 0.0f
  var rotationRoll = 0.0f
  val ShipWorld: ShipWorld = new ShipWorld(world, shipBlockPos, blockSet, this)


  // Returns ship direction based on which way the creator block is facing
  val ShipDirection: EnumFacing = if (ShipWorld.isValid) ShipWorld.ShipBlock.getValue(ShipCreatorBlock.FACING) else null

  private def directionVec = ShipDirection.getDirectionVec
  var Rotation: Quat4f = new Quat4f(0,0,1f,1)


  posX = shipBlockPos.getX
  posY = shipBlockPos.getY
  posZ = shipBlockPos.getZ


  if (blockSet.nonEmpty) ShipEntity.nextID.set(ShipEntity.nextID.get + 1)

  private var boundingBox = ShipWorld.genBoundingBox()
  private var relativeBoundingBox = ShipWorld.genRelativeBoundingBox()

  var RotatedBB = new RotatedBB(getEntityBoundingBox, new Vec3(ShipBlockPos.getX, ShipBlockPos.getY, ShipBlockPos.getZ), rotationMatrix)


  def this(world: World) = this(new BlockPos(0, 0, 0), world, Set[BlockPos](), new BlockPos(0, 0, 0))
  // Returns the axis aligned bounding box relative to the world
  override def getEntityBoundingBox = if (ShipWorld.isValid) boundingBox else new AxisAlignedBB(0, 0, 0, 0, 0, 0)

  def getRelativeRotated = {
    new RotatedBB(getRelativeBoundingBox, new Vec3(0.5, 0.5, 0.5), rotationMatrix)
  }
  // Returns an axis aligned bounding box relative to the ship's creator block
  def getRelativeBoundingBox = relativeBoundingBox




  def correctionMatrix: Matrix4f = {
    def rotationOffset(direction: EnumFacing): Float = direction match {
      case EnumFacing.EAST => 0f
      case EnumFacing.WEST => 180f
      case EnumFacing.SOUTH => 90f
      case EnumFacing.NORTH => 270f
      case _ => 0f
    }

    // # Correction Matrix #
    // Basically rotates the matrix so that our rotation is relative to the Ship Block's front face.
    // We also never have to rotate our axis upwards or downwards so we only need to rotate the yaw (pitch) value.
    val rotCorrectionMatrix = new Matrix4f()
    rotCorrectionMatrix.rotY(Math.toRadians(rotationOffset(ShipDirection)).toFloat)

    rotCorrectionMatrix
  }

  // THIS IS UNRELATED TO THE BACKWARDS YAW PROBLEM
  private def rotationCorrectionMatrix: Matrix3f = {
    def rotationOffset(direction: EnumFacing): Float = direction match {
      case EnumFacing.EAST => 180f
      case EnumFacing.WEST => 0f
      case EnumFacing.SOUTH => 90f
      case EnumFacing.NORTH => 270f
      case _ => 0f
    }
    val rotCorrectionMatrix = new Matrix3f()
    rotCorrectionMatrix.rotY(Math.toRadians(rotationOffset(ShipDirection)).toFloat)

    rotCorrectionMatrix
  }

  private def rotationMatrix: Matrix3f = {
    val cMatrix = rotationCorrectionMatrix
    val rMatrix = new Matrix3f()
    renderMatrix.getRotationScale(rMatrix)

    cMatrix.mul(rMatrix)
    cMatrix
  }

  def renderMatrix: Matrix4f = {
    val xMat: Matrix4f = new Matrix4f()
    xMat.rotX(Math.toRadians(rotationRoll).toFloat)

    // Switch Y and Z cause in Minecraft the Y axis is "up"
    val yMat: Matrix4f = new Matrix4f()
    yMat.rotZ(Math.toRadians(rotationPitch).toFloat)
    // See above ^
    val zMat: Matrix4f = new Matrix4f()
    zMat.rotY(Math.toRadians(rotationYaw).toFloat)

    // Multiply our rotation matrices into one unified rotation matrix
    xMat.mul(zMat)
    yMat.mul(xMat)

    // Odd multiplying syntax, but yMat has all the rotations in it
    yMat
  }
  override def writeEntityToNBT(tagCompound: NBTTagCompound): Unit = {

  }

  override def readEntityFromNBT(tagCompound: NBTTagCompound): Unit = {

  }

  override def entityInit(): Unit = {
  }
  override def onUpdate() = {
    if (!ShipWorld.isValid) {
      this.setDead()
    }
    setPositionAndRotation(posX, posY, posZ, rotationYaw+1f, 0)
    prevRotationRoll = rotationRoll
    rotationRoll = 0
    RotatedBB = RotatedBB.moveTo(posX,posY,posZ)
  }

  override def setPositionAndRotation(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) = {
    setPosition(x, y, z)
    prevRotationYaw = rotationYaw
    prevRotationPitch = rotationPitch

    rotationYaw = yaw % 360
    rotationPitch = pitch % 360
    RotatedBB = RotatedBB.rotate(rotationMatrix)
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
      RotatedBB = RotatedBB.moveTo(x, y, z)
    }
    // Update positions
    posX = x
    posY = y
    posZ = z

  }

  override def canBeCollidedWith = true

  override def interactAt(player: EntityPlayer, target: Vec3) = InteractionHandler.interactionFired(player, target)




}
