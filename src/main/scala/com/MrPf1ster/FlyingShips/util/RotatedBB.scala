package com.MrPf1ster.FlyingShips.util

import javax.vecmath.Matrix3f

import net.minecraft.util.{AxisAlignedBB, Vec3}

/**
  * Created by EJ on 3/13/2016.
  */
case class RotatedBB(MinPos: Vec3, MaxPos: Vec3, RotationPoint: Vec3, RotationMatrix: Matrix3f) {
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
      multiplyPointByMatrix(corner.subtract(RotationPoint), RotationMatrix).add(RotationPoint)
    })
  }

  private def multiplyPointByMatrix(P: Vec3, M: Matrix3f): Vec3 = {
    new Vec3(
      P.xCoord * M.m00 + P.yCoord * M.m01 + P.zCoord * M.m02,
      P.xCoord * M.m10 + P.yCoord * M.m11 + P.zCoord * M.m12,
      P.xCoord * M.m20 + P.yCoord * M.m21 + P.zCoord * M.m22
    )
  }

  def this(BB: AxisAlignedBB, RotationPoint: Vec3, RotationMatrix: Matrix3f) = this(new Vec3(BB.minX, BB.minY, BB.minZ), new Vec3(BB.maxX, BB.maxY, BB.maxZ), RotationPoint, RotationMatrix)

  def BackBottomLeft: Vec3 = Corners(0)

  def BackBottomRight: Vec3 = Corners(1)

  def BackTopLeft: Vec3 = Corners(2)

  def BackTopRight: Vec3 = Corners(3)

  def ForwardBottomLeft: Vec3 = Corners(4)

  def ForwardBottomRight: Vec3 = Corners(5)

  def ForwardTopLeft: Vec3 = Corners(6)

  def ForwardTopRight: Vec3 = Corners(7)

  def rotate(newMatrix: Matrix3f): RotatedBB = this.copy(MinPos, MaxPos, RotationPoint, newMatrix)

  def moveTo(x: Double, y: Double, z: Double): RotatedBB = {
    val add = new Vec3(x - MinPos.xCoord, y - MinPos.yCoord, z - MinPos.zCoord)
    this.copy(MinPos.add(add), MaxPos.add(add), RotationPoint, RotationMatrix)
  }

  private def deltaX = MaxPos.xCoord - MinPos.xCoord

  private def deltaY = MaxPos.yCoord - MinPos.yCoord

  private def deltaZ = MaxPos.zCoord - MinPos.zCoord

}
