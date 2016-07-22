package mrpf1ster.flyingships.world

import mrpf1ster.flyingships.util.{UnifiedVec, VectorUtils}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{MathHelper, Vec3}

/**
  * Created by EJ on 7/19/2016.
  */
case class PlayerRelative(player: EntityPlayer, shipWorld: ShipWorld) extends EntityPlayer(shipWorld, player.getGameProfile) {
  override def isSpectator: Boolean = player.isSpectator

  val relPosition = UnifiedVec.convertToRelative(player.posX, player.posY, player.posZ, shipWorld.OriginVec())
  val relLastTick = UnifiedVec.convertToRelative(player.lastTickPosX, player.lastTickPosY, player.lastTickPosZ, shipWorld.OriginVec())
  posX = relPosition.xCoord
  posY = relPosition.yCoord
  posZ = relPosition.zCoord
  lastTickPosX = relLastTick.xCoord
  lastTickPosY = relLastTick.yCoord
  lastTickPosZ = relLastTick.zCoord

  override def getDistanceSq(x: Double, y: Double, z: Double): Double = {

    val rotatedVec = VectorUtils.rotatePointToShip(new Vec3(x, y, z), shipWorld.Ship)
    val d0: Double = this.posX - rotatedVec.xCoord
    val d1: Double = this.posY - rotatedVec.yCoord
    val d2: Double = this.posZ - rotatedVec.zCoord

    d0 * d0 + d1 * d1 + d2 * d2
  }

  override def getDistance(x: Double, y: Double, z: Double) = MathHelper.sqrt_double(getDistanceSq(x, y, z))
}
