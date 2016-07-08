package mrpf1ster.flyingships.util

import net.minecraft.util.BlockPos

/**
  * Created by EJ on 3/11/2016.
  */
object UnifiedPos {
  def convertToRelative(pos:BlockPos,origin:BlockPos) = pos.subtract(origin)
  def convertToWorld(pos:BlockPos, origin:BlockPos) = pos.add(origin)
}
case class UnifiedPos(Position: BlockPos, Origin: () => BlockPos, IsRelative: Boolean = false) {

  def this(x: Double, y: Double, z: Double, Origin: () => BlockPos, relative: Boolean) = this(new BlockPos(x, y, z), Origin, relative)

  def WorldPos = if (!IsRelative) Position else UnifiedPos.convertToWorld(Position,Origin())

  def WorldPosX: Int = WorldPos.getX

  def WorldPosY: Int = WorldPos.getY

  def WorldPosZ: Int = WorldPos.getZ

  def RelativePos = if (IsRelative) Position else UnifiedPos.convertToRelative(Position,Origin())

  def RelPosX: Int = RelativePos.getX

  def RelPosY: Int = RelativePos.getY

  def RelPosZ: Int = RelativePos.getZ

  override def toString: String = s"UnifiedPos[World = $WorldPos,Relative = $RelativePos]"

  override def equals(other: Any):Boolean = other match {
    case x:UnifiedPos => x.RelativePos.equals(this.RelativePos)
    case _ => false
  }
  override def hashCode():Int = (this.RelPosY + this.RelPosZ * 31) * 31 + this.RelPosX


}