package com.MrPf1ster.FlyingShips.util

import javax.vecmath.Quat4f

import net.minecraft.util.{AxisAlignedBB, Vec3}

/**
  * Created by EJ on 3/13/2016.
  */
case class RotatedBB(MinPos: Vec3, MaxPos: Vec3, RotationPoint: Vec3, Rotation: Quat4f) {

  private def deltaX = MaxPos.xCoord - MinPos.xCoord

  private def deltaY = MaxPos.yCoord - MinPos.yCoord

  private def deltaZ = MaxPos.zCoord - MinPos.zCoord

  val Corners: Array[Vec3] = {
    val a = Array(
      MinPos, // Back Bottom Left
      MinPos.addVector(deltaX, 0, 0), // Back Bottom Right
      MinPos.addVector(0, deltaY, 0), // Back Top Left
      MaxPos.subtract(0, 0, deltaZ), // Back Top Right
      MinPos.addVector(0, 0, deltaZ), // Forward Bottom Left
      MaxPos.subtract(0, deltaY, 0), // Forward Bottom Right
      MaxPos.subtract(deltaX, 0, 0), // Forward Top Left
      MaxPos) // Forward Top Right

    a map (corner => {
      rotatePointByQuaternion(corner.subtract(RotationPoint), Rotation).add(RotationPoint)
    })
  }

  private def rotatePointByQuaternion(P: Vec3, Q: Quat4f): Vec3 = {
    // Straight from Stack Overflow, -- https://stackoverflow.com/questions/9892400/java-3d-rotation-with-quaternions
    val x_old = P.xCoord
    val y_old = P.yCoord
    val z_old = P.zCoord

    val w = Q.getW
    val x = Q.getX
    val y = Q.getY
    val z = Q.getZ

    val x_new = ((1 - 2 * y * y - 2 * z * z) * x_old + (2 * x * y + 2 * w * z) * y_old + (2 * x * z - 2 * w * y) * z_old).toFloat
    val y_new = ((2 * x * y - 2 * w * z) * x_old + (1 - 2 * x * x - 2 * z * z) * y_old + (2 * y * z + 2 * w * x) * z_old).toFloat
    val z_new = ((2 * x * z + 2 * w * y) * x_old + (2 * y * z - 2 * w * x) * y_old + (1 - 2 * x * x - 2 * y * y) * z_old).toFloat

    new Vec3(x_new, y_new, z_new)
  }

  def this(BB: AxisAlignedBB, RotationPoint: Vec3, Rotation: Quat4f) = this(new Vec3(BB.minX, BB.minY, BB.minZ), new Vec3(BB.maxX, BB.maxY, BB.maxZ), RotationPoint, Rotation)

  def BackBottomLeft: Vec3 = Corners(0)

  def BackBottomRight: Vec3 = Corners(1)

  def BackTopLeft: Vec3 = Corners(2)

  def BackTopRight: Vec3 = Corners(3)

  def ForwardBottomLeft: Vec3 = Corners(4)

  def ForwardBottomRight: Vec3 = Corners(5)

  def ForwardTopLeft: Vec3 = Corners(6)

  def ForwardTopRight: Vec3 = Corners(7)

  // Returns RotatedBB with new rotation
  def rotateTo(Rotation: Quat4f): RotatedBB = this.copy(MinPos, MaxPos, RotationPoint, Rotation)

  // Returns RotatedBB with rotation multiplied by the delta rotation
  def rotateBy(DeltaRotation: Quat4f): RotatedBB = {
    Rotation.mul(DeltaRotation)
    this.copy(MinPos, MaxPos, RotationPoint, Rotation)
  }

  // Returns new RotatedBB with position changed
  def moveTo(x: Double, y: Double, z: Double): RotatedBB = {
    val add = new Vec3(x - MinPos.xCoord, y - MinPos.yCoord, z - MinPos.zCoord)
    this.copy(MinPos.add(add), MaxPos.add(add), RotationPoint, Rotation)
  }


}
