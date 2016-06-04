package com.MrPf1ster.FlyingShips.util

import javax.vecmath.Quat4f

import net.minecraft.util.Vec3

/**
  * Created by EJ on 6/3/2016.
  */
object VectorUtils {
  def rotatePointByQuaternion(P: Vec3, Q: Quat4f): Vec3 = {
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
}
