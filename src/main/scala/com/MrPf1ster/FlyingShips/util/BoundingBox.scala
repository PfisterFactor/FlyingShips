package com.MrPf1ster.FlyingShips.util

import java.lang.Math._
import javax.vecmath.Quat4f

import net.minecraft.util.{AxisAlignedBB, BlockPos, Vec3}

/**
  * Created by EJ on 6/3/2016.
  */
object BoundingBox {
  private def findMinMaxBlock(BlockSet: Set[UnifiedPos], isRelative: Boolean): (BlockPos, BlockPos) = {
    var minX = Int.MaxValue
    var minY = Int.MaxValue
    var minZ = Int.MaxValue
    var maxX = Int.MinValue
    var maxY = Int.MinValue
    var maxZ = Int.MinValue

    BlockSet.foreach(uPos => {
      def pos: BlockPos = if (isRelative) uPos.RelativePos else uPos.WorldPos

      minX = min(minX, pos.getX)
      minY = min(minY, pos.getY)
      minZ = min(minZ, pos.getZ)

      maxX = max(maxX, pos.getX)
      maxY = max(maxY, pos.getY)
      maxZ = max(maxZ, pos.getZ)


    })

    val minPos = new BlockPos(minX, minY, minZ)
    val maxPos = new BlockPos(maxX + 1, maxY + 1, maxZ + 1)

    (minPos, maxPos)
  }

  def generateRotated(BlockSet: Set[UnifiedPos], Rotation: Quat4f): RotatedBB = {
    val minMax = findMinMaxBlock(BlockSet, isRelative = false)
    new RotatedBB(new AxisAlignedBB(minMax._1, minMax._2), new Vec3(0.5, 0.5, 0.5), Rotation)
  }

  def generateRotatedRelative(BlockSet: Set[UnifiedPos], Rotation: Quat4f): RotatedBB = {
    val minMax = findMinMaxBlock(BlockSet, isRelative = true)
    new RotatedBB(new AxisAlignedBB(minMax._1, minMax._2), new Vec3(0.5, 0.5, 0.5), Rotation)
  }
}

case class BoundingBox(RBB: RotatedBB, RelativeRBB: RotatedBB, Rotation: Quat4f, ShipPos: Vec3) {


  val RelativeMinPos = new Vec3(RelativeRBB.Corners.minBy(v => v.xCoord).xCoord, RelativeRBB.Corners.minBy(v => v.yCoord).yCoord, RelativeRBB.Corners.minBy(v => v.zCoord).zCoord)

  val RelativeMaxPos = new Vec3(RelativeRBB.Corners.maxBy(v => v.xCoord).xCoord, RelativeRBB.Corners.maxBy(v => v.yCoord).yCoord, RelativeRBB.Corners.maxBy(v => v.zCoord).zCoord)

  val MinPos = RelativeMinPos.add(ShipPos)

  val MaxPos = RelativeMaxPos.add(ShipPos)

  def AABB: AxisAlignedBB = new AxisAlignedBB(MinPos.xCoord, MinPos.yCoord, MinPos.zCoord, MaxPos.xCoord, MaxPos.yCoord, MaxPos.zCoord)

  def RelativeAABB: AxisAlignedBB = new AxisAlignedBB(RelativeMinPos.xCoord, RelativeMinPos.yCoord, RelativeMinPos.zCoord, RelativeMaxPos.xCoord, RelativeMaxPos.yCoord, RelativeMaxPos.zCoord)

  def moveTo(X: Double, Y: Double, Z: Double) = this.copy(RBB.moveTo(X, Y, Z), RelativeRBB, Rotation, ShipPos.addVector(X - ShipPos.xCoord, Y - ShipPos.yCoord, Z - ShipPos.zCoord))

  def moveTo(pos: Vec3): BoundingBox = this.moveTo(pos.xCoord, pos.yCoord, pos.zCoord)

  // Returns BoundingBox with new rotation
  def rotateTo(Rotation: Quat4f): BoundingBox = this.copy(RBB.rotateTo(Rotation), RelativeRBB.rotateTo(Rotation), Rotation)

  // Returns BoundingBox with rotation multiplied by the delta rotation
  def rotateBy(DeltaRotation: Quat4f): BoundingBox = {
    Rotation.mul(DeltaRotation)
    this.copy(RBB.rotateTo(Rotation), RelativeRBB.rotateTo(Rotation), Rotation)
  }
}
