package mrpf1ster.flyingships.util

import javax.vecmath.Quat4f

import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.world.ShipWorld
import net.minecraft.util.Vec3

/**
  * Created by EJ on 6/3/2016.
  */
object VectorUtils {
  // Rotates the (relative to ship!) vec by the ships rotation and then translates it to a world position
  def getRotatedWorldVec(vec: Vec3, entityShip: EntityShip) = UnifiedVec.convertToWorld(VectorUtils.rotatePointToShip(vec.xCoord, vec.yCoord, vec.zCoord, entityShip), entityShip.getPositionVector)

  def getRotatedWorldVec(x: Double, y: Double, z: Double, entityShip: EntityShip): Vec3 = getRotatedWorldVec(new Vec3(x, y, z), entityShip)

  // Returns an scalar representing the "closeness" of two quaternions, 0 (or near it) for the same rotation to 1 if their 180 deg apart
  def angularDifference(A: Quat4f, B: Quat4f): Double = {
    A.normalize()
    B.normalize()
    val dotProduct = A.x * B.x + A.y * B.y + A.z * B.z + A.w * B.w

    1 - (dotProduct * dotProduct)
  }
  def rotatePointByQuaternion(P: Vec3, Q: Quat4f): Vec3 = {

    // Straight from Stack Overflow, -- https://stackoverflow.com/questions/9892400/java-3d-rotation-with-quaternions
    val x_old = P.xCoord
    val y_old = P.yCoord
    val z_old = P.zCoord

    val w = Q.getW
    val x = Q.getX
    val y = Q.getY
    val z = Q.getZ

    val x_new =  (1 - 2*y*y -2*z*z)*x_old + (2*x*y + 2*w*z)*y_old + (2*x*z-2*w*y)*z_old
    val y_new =  (2*x*y - 2*w*z)*x_old + (1 - 2*x*x - 2*z*z)*y_old + (2*y*z + 2*w*x)*z_old
    val z_new =  (2*x*z + 2*w*y)*x_old + (2*y*z - 2*w*x)*y_old + (1 - 2*x*x - 2*y*y)*z_old



    new Vec3(x_new, y_new, z_new)
  }

  // Assumes P is relative to ship
  def rotatePointToShip(P: Vec3, Ship: EntityShip, useInterpolated: Boolean = false): Vec3 = {
    if (useInterpolated)
      rotatePointByQuaternion(P.subtract(0.5, 0.5, 0.5).subtract(ShipWorld.ShipBlockVec), Ship.interpolatedRotation).addVector(0.5, 0.5, 0.5).add(ShipWorld.ShipBlockVec)
    else
      rotatePointByQuaternion(P.subtract(0.5, 0.5, 0.5).subtract(ShipWorld.ShipBlockVec), Ship.getRotation).addVector(0.5, 0.5, 0.5).add(ShipWorld.ShipBlockVec)
  }


  def rotatePointToShip(x: Double, y: Double, z: Double, Ship: EntityShip): Vec3 = rotatePointToShip(new Vec3(x, y, z), Ship)

  // Assumes P is relative to ship
  def rotatePointFromShip(P: Vec3, Ship: EntityShip, useInterpolated: Boolean = false): Vec3 = {
    var inversedRot: Quat4f = null
    if (useInterpolated)
      inversedRot = Ship.interpolatedRotation.clone().asInstanceOf[Quat4f] // clone because inverse mutates
    else
      inversedRot = Ship.getRotation.clone().asInstanceOf[Quat4f] // clone because inverse mutates

    inversedRot.inverse()

    rotatePointByQuaternion(P.subtract(0.5, 0.5, 0.5).subtract(ShipWorld.ShipBlockVec), inversedRot).addVector(0.5, 0.5, 0.5).add(ShipWorld.ShipBlockVec)
  }

  def rotatePointFromShip(x: Double, y: Double, z: Double, Ship: EntityShip): Vec3 = rotatePointFromShip(new Vec3(x, y, z), Ship)
}
