package com.MrPf1ster.FlyingShips.util

import net.minecraft.util.Vec3

/**
  * Created by EJ on 4/13/2016.
  */
case class UnifiedVec(Vector: Vec3, Origin: Vec3, IsRelative: Boolean) {

  def this(X: Double, Y: Double, Z: Double, Origin: Vec3, IsRelative: Boolean) = this(new Vec3(X, Y, Z), Origin, IsRelative)

  def WorldVec = if (!IsRelative) Vector else Vector.add(Origin)

  def WorldVecX: Double = WorldVec.xCoord

  def WorldVecY: Double = WorldVec.yCoord

  def WorldVecZ: Double = WorldVec.zCoord

  def RelativeVec = if (IsRelative) Vector else Vector.subtract(Origin)

  def RelVecX: Double = RelativeVec.xCoord

  def RelVecY: Double = RelativeVec.yCoord

  def RelVecZ: Double = RelativeVec.zCoord

  override def toString: String = s"UnifiedPos[World = $WorldVec,Relative = $RelativeVec]"
}