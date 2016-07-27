package mrpf1ster.flyingships.util

import javax.vecmath.Quat4f

import mrpf1ster.flyingships.world.ShipWorld
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
      VectorUtils.rotatePointByQuaternion(corner.subtract(RotationPoint).subtract(ShipWorld.ShipBlockVec), Rotation).add(RotationPoint).add(ShipWorld.ShipBlockVec)
    })
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

  // Moves RotatedBB to position
  def moveTo(x: Double, y: Double, z: Double): RotatedBB = {
    val add = new Vec3(x - MinPos.xCoord, y - MinPos.yCoord, z - MinPos.zCoord)
    this.copy(MinPos.add(add), MaxPos.add(add), RotationPoint, Rotation)
  }

  // Rotates RotatedBB to rotation
  def rotateTo(Rotation: Quat4f): RotatedBB = this.copy(MinPos, MaxPos, RotationPoint, Rotation)

  // Rotates RotatedBB by rotation (multiplicative)
  def rotateBy(DeltaRotation: Quat4f): RotatedBB = {
    Rotation.mul(DeltaRotation)
    this.copy(MinPos, MaxPos, RotationPoint, Rotation)
  }

  // Expands RotatedBB by delta
  def expand(delta:Double): RotatedBB = this.copy(MinPos.addVector(-delta,-delta,-delta), MaxPos.addVector(delta,delta,delta))

  // Offsets RotatedBB by delta
  def offset(xDelta:Double, yDelta:Double, zDelta:Double):RotatedBB = this.copy(new Vec3(this.MinPos.xCoord + xDelta, this.MinPos.yCoord + yDelta, this.MinPos.zCoord + zDelta), new Vec3(this.MaxPos.xCoord + xDelta, this.MaxPos.yCoord + yDelta, this.MaxPos.zCoord + zDelta))

  def offset(delta:Vec3):RotatedBB = offset(delta.xCoord,delta.yCoord,delta.zCoord)




}
