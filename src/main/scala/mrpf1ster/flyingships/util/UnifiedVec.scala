package mrpf1ster.flyingships.util

import net.minecraft.util.Vec3

/**
  * Created by EJ on 4/13/2016.
  */
object UnifiedVec {
  def convertToRelative(vec:Vec3,origin:Vec3) = vec.subtract(origin)
  def convertToWorld(vec:Vec3, origin:Vec3) = vec.add(origin)
}
case class UnifiedVec(Vector: Vec3, Origin: () => Vec3, IsRelative: Boolean) {

  def this(X: Double, Y: Double, Z: Double, Origin: () => Vec3, IsRelative: Boolean) = this(new Vec3(X, Y, Z), Origin, IsRelative)

  def WorldVec = if (!IsRelative) Vector else UnifiedVec.convertToWorld(Vector,Origin())

  def WorldVecX: Double = WorldVec.xCoord

  def WorldVecY: Double = WorldVec.yCoord

  def WorldVecZ: Double = WorldVec.zCoord

  def RelativeVec = if (IsRelative) Vector else UnifiedVec.convertToRelative(Vector,Origin())

  def RelVecX: Double = RelativeVec.xCoord

  def RelVecY: Double = RelativeVec.yCoord

  def RelVecZ: Double = RelativeVec.zCoord

  override def equals(other: Any):Boolean = other match {
    case x:UnifiedVec => x.RelativeVec.equals(this.RelativeVec)
    case _ => false
  }

  override def toString: String = s"UnifiedPos[World = $WorldVec,Relative = $RelativeVec]"
}
