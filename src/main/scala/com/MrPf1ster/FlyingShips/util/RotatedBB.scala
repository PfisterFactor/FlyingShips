package com.MrPf1ster.FlyingShips.util

import net.minecraft.util.{AxisAlignedBB, MathHelper, Vec3}

/**
  * Created by EJ on 3/13/2016.
  */
class RotatedBB(minPos: Vec3, maxPos: Vec3, rotationPoint: Vec3, rotationOffset: Float, yaw: Float, pitch: Float, roll: Float) {
  def this(bb: AxisAlignedBB, rotationPoint: Vec3, rotationOffset: Float, yaw: Float, pitch: Float, roll: Float) = this(new Vec3(bb.minX, bb.minY, bb.minZ), new Vec3(bb.maxX, bb.maxY, bb.maxZ), rotationPoint, rotationOffset, yaw, pitch, roll)


  private def deltaX = maxPos.xCoord - minPos.xCoord

  private def deltaY = maxPos.yCoord - minPos.yCoord

  private def deltaZ = maxPos.zCoord - minPos.zCoord



  var RotationPoint = rotationPoint
  val RotationOffset = rotationOffset

  val Corners: Array[Vec3] = Array(
    minPos, // Back Bottom Left
    minPos.addVector(deltaX, 0, 0), // Back Bottom Right
    minPos.addVector(0, deltaY, 0), // Back Top Left
    maxPos.subtract(0, 0, deltaZ), // Back Top Right
    minPos.addVector(0, 0, deltaZ), // Forward Bottom Left
    maxPos.subtract(0, deltaY, 0), // Forward Bottom Right
    maxPos.subtract(deltaX, 0, 0), // Forward Top Left
    maxPos) // Forward Top Right


  def BackBottomLeft: Vec3 = Corners(0)

  def BackBottomRight: Vec3 = Corners(1)

  def BackTopLeft: Vec3 = Corners(2)

  def BackTopRight: Vec3 = Corners(3)

  def ForwardBottomLeft: Vec3 = Corners(4)

  def ForwardBottomRight: Vec3 = Corners(5)

  def ForwardTopLeft: Vec3 = Corners(6)

  def ForwardTopRight: Vec3 = Corners(7)

  def MinPos = BackBottomLeft

  def MaxPos = ForwardTopRight

  private var prevRotationYaw = 0.0f
  private var prevRotationPitch = 0.0f
  private var prevRotationRoll = 0.0f

  private var rotationYaw = yaw
  private var rotationPitch = pitch
  private var rotationRoll = roll

  def getYaw = rotationYaw

  def getPitch = rotationPitch

  def getRoll = rotationRoll

  updateCorners()

  // I have no idea if this works :I
  private def rotateVecRoll(vector: Vec3, roll: Float): Vec3 = {
    val cos: Float = MathHelper.cos(roll)
    val sin: Float = MathHelper.sin(roll)
    val d0: Double = vector.xCoord * cos.toDouble - vector.yCoord * sin.toDouble
    val d1: Double = vector.xCoord * sin.toDouble + vector.yCoord * cos.toDouble
    val d2: Double = vector.zCoord
    return new Vec3(d0, d1, d2)
  }

  def setRotation(yaw: Float, pitch: Float, roll: Float) = {
    prevRotationYaw = rotationYaw
    prevRotationPitch = rotationPitch
    prevRotationRoll = rotationRoll
    rotationYaw = yaw % 360.0f
    rotationRoll = roll % 360.0f
    rotationPitch = pitch % 360.0f
    updateCorners()
  }

  def setPosition(x: Double, y: Double, z: Double): Unit = {
    RotationPoint = RotationPoint.addVector(x - RotationPoint.xCoord, y - RotationPoint.yCoord, z - RotationPoint.zCoord)
    Corners.foreach(pos => pos.add(new Vec3(x - pos.xCoord, y - pos.yCoord, z - pos.zCoord)))
  }

  def updateCorners() = {
    val radiansYaw: Float = (rotationYaw * (Math.PI / 180.0F)).toFloat
    val radiansPitch: Float = (-rotationPitch * (Math.PI / 180.0f)).toFloat
    val radiansRoll: Float = (-rotationRoll * (Math.PI / 180.0f)).toFloat

    val radiansIdontknow = Math.PI.toFloat
    val radiansOffset = (RotationOffset) * (Math.PI / 180.0f).toFloat

    for (i <- 0 until Corners.length) {
      Corners(i) = Corners(i).subtract(RotationPoint)
      var rotatedCorner = Corners(i)


      rotatedCorner = rotateVecRoll(rotatedCorner, radiansRoll)
      rotatedCorner = rotatedCorner.rotateYaw(radiansYaw).rotatePitch(radiansPitch)








      Corners(i) = rotatedCorner
      Corners(i) = Corners(i).add(RotationPoint)
    }
  }

}
