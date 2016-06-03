package com.MrPf1ster.FlyingShips.util

import javax.vecmath.Quat4f

import net.minecraft.util.{AxisAlignedBB, Vec3}

/**
  * Created by EJ on 3/13/2016.
  */

object UnifiedBB {

  def Empty: UnifiedBB = new UnifiedBB(new UnifiedVec(0, 0, 0, new Vec3(0, 0, 0), true), new UnifiedVec(0, 0, 0, new Vec3(0, 0, 0), true), new Vec3(0, 0, 0), new Quat4f(0, 0, 0, 1))
}

case class UnifiedBB(MinPos: UnifiedVec, MaxPos: UnifiedVec, RotationPoint: Vec3, Rotation: Quat4f) {

  private def deltaX = MaxPos.RelVecX - MinPos.RelVecX

  private def deltaY = MaxPos.RelVecY - MinPos.RelVecY

  private def deltaZ = MaxPos.RelVecZ - MinPos.RelVecZ

  def RelativeAABB = new AxisAlignedBB(MinPos.RelVecX, MinPos.RelVecY, MinPos.RelVecZ, MaxPos.RelVecX, MaxPos.RelVecY, MaxPos.RelVecZ)

  def WorldAABB = new AxisAlignedBB(MinPos.WorldVecX, MinPos.WorldVecY, MinPos.WorldVecZ, MaxPos.WorldVecX, MaxPos.WorldVecY, MaxPos.WorldVecZ)

  val Corners: Array[UnifiedVec] = {
    val a = Array(
      MinPos, // Back Bottom Left
      MinPos.addVector(deltaX, 0, 0), // Back Bottom Right
      MinPos.addVector(0, deltaY, 0), // Back Top Left
      MaxPos.subtractVector(0, 0, deltaZ), // Back Top Right
      MinPos.addVector(0, 0, deltaZ), // Forward Bottom Left
      MaxPos.subtractVector(0, deltaY, 0), // Forward Bottom Right
      MaxPos.subtractVector(deltaX, 0, 0), // Forward Top Left
      MaxPos) // Forward Top Right

    a map (corner => {
      rotatePointByQuaternion(corner.subtract(RotationPoint), Rotation).add(RotationPoint)
    })
  }

  private def rotatePointByQuaternion(P: UnifiedVec, Q: Quat4f): UnifiedVec = {
    // Straight from Stack Overflow, -- https://stackoverflow.com/questions/9892400/java-3d-rotation-with-quaternions
    val x_old = P.RelVecX
    val y_old = P.RelVecY
    val z_old = P.RelVecZ

    val w = Q.getW
    val x = Q.getX
    val y = Q.getY
    val z = Q.getZ

    val x_new = ((1 - 2 * y * y - 2 * z * z) * x_old + (2 * x * y + 2 * w * z) * y_old + (2 * x * z - 2 * w * y) * z_old).toFloat
    val y_new = ((2 * x * y - 2 * w * z) * x_old + (1 - 2 * x * x - 2 * z * z) * y_old + (2 * y * z + 2 * w * x) * z_old).toFloat
    val z_new = ((2 * x * z + 2 * w * y) * x_old + (2 * y * z - 2 * w * x) * y_old + (1 - 2 * x * x - 2 * y * y) * z_old).toFloat

    new UnifiedVec(x_new, y_new, z_new, P.Origin, P.IsRelative)
  }


  def BackBottomLeft: UnifiedVec = Corners(0)

  def BackBottomRight: UnifiedVec = Corners(1)

  def BackTopLeft: UnifiedVec = Corners(2)

  def BackTopRight: UnifiedVec = Corners(3)

  def ForwardBottomLeft: UnifiedVec = Corners(4)

  def ForwardBottomRight: UnifiedVec = Corners(5)

  def ForwardTopLeft: UnifiedVec = Corners(6)

  def ForwardTopRight: UnifiedVec = Corners(7)

  // Returns RotatedBB with new rotation
  def rotateTo(Rotation: Quat4f): UnifiedBB = this.copy(MinPos, MaxPos, RotationPoint, Rotation)

  // Returns RotatedBB with rotation multiplied by the delta rotation
  def rotateBy(DeltaRotation: Quat4f): UnifiedBB = {
    Rotation.mul(DeltaRotation)
    this.copy(MinPos, MaxPos, RotationPoint, Rotation)
  }

  // Returns new RotatedBB with position changed
  def moveTo(x: Double, y: Double, z: Double): UnifiedBB = {
    val add = new Vec3(x - MinPos.WorldVecX, y - MinPos.WorldVecY, z - MinPos.WorldVecZ)
    this.copy(MinPos.add(add), MaxPos.add(add), RotationPoint, Rotation)
  }


}
