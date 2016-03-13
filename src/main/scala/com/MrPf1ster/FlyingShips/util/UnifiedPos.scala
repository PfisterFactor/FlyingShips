package com.MrPf1ster.FlyingShips.util

import com.MrPf1ster.FlyingShips.entities.ShipEntity
import net.minecraft.util.{BlockPos, MathHelper, Vec3}

/**
  * Created by EJ on 3/11/2016.
  */

class UnifiedPos(x: Double, y: Double, z: Double, ship: ShipEntity, relative: Boolean) {

  def this(pos: BlockPos, ship: ShipEntity, relative: Boolean) = this(pos.getX, pos.getY, pos.getZ, ship, relative)

  def this(pos: Vec3, ship: ShipEntity, relative: Boolean) = this(pos.xCoord, pos.yCoord, pos.zCoord, ship, relative)


  def ShipBlockPos = ship.ShipBlockPos

  private var isRelative = relative
  private var storedVec = new Vec3(x, y, z)

  def storedPos = new BlockPos(MathHelper.floor_double(storedVec.xCoord), MathHelper.floor_double(storedVec.yCoord), MathHelper.floor_double(storedVec.zCoord))

  def IsRelative = isRelative


  def setPos(x: Double, y: Double, z: Double) = {
    storedVec = new Vec3(x, y, z)
  }

  def setPos(pos: Vec3) = {
    storedVec = pos
  }

  def setPos(pos: BlockPos) = {
    storedVec = new Vec3(pos)
  }

  def setRelative(rel: Boolean) = {
    isRelative = rel
  }


  def WorldPos = if (!isRelative) storedPos else storedPos.add(ShipBlockPos)

  def RelativePos = if (isRelative) storedPos else storedPos.subtract(ShipBlockPos)

  def WorldVec = if (!isRelative) storedVec else storedVec.addVector(ShipBlockPos.getX, ShipBlockPos.getY, ShipBlockPos.getZ)

  def RelativeVec = if (isRelative) storedVec else storedVec.subtract(ShipBlockPos.getX, ShipBlockPos.getY, ShipBlockPos.getZ)

  // AHHH
  def WorldPosX: Int = if (isRelative) storedPos.getX + ShipBlockPos.getX else storedPos.getX

  def WorldPosY: Int = if (isRelative) storedPos.getY + ShipBlockPos.getY else storedPos.getY

  def WorldPosZ: Int = if (isRelative) storedPos.getZ + ShipBlockPos.getZ else storedPos.getZ

  def RelPosX: Int = if (!isRelative) storedPos.getX - ShipBlockPos.getX else storedPos.getX

  def RelPosY: Int = if (!isRelative) storedPos.getY - ShipBlockPos.getY else storedPos.getY

  def RelPosZ: Int = if (!isRelative) storedPos.getZ - ShipBlockPos.getZ else storedPos.getZ

  def WorldVecX: Double = if (isRelative) storedVec.xCoord + ShipBlockPos.getX else storedVec.xCoord

  def WorldVecY: Double = if (isRelative) storedVec.yCoord + ShipBlockPos.getY else storedVec.yCoord

  def WorldVecZ: Double = if (isRelative) storedVec.zCoord + ShipBlockPos.getZ else storedVec.zCoord

  def RelVecX: Double = if (!isRelative) storedVec.xCoord - ShipBlockPos.getX else storedVec.xCoord

  def RelVecY: Double = if (!isRelative) storedVec.yCoord - ShipBlockPos.getY else storedVec.yCoord

  def RelVecZ: Double = if (!isRelative) storedVec.zCoord - ShipBlockPos.getZ else storedVec.zCoord


  override def toString: String = s"UnifiedPos[World = $WorldVec,Relative = $RelativeVec]"

  override def hashCode = (RelPosY + RelPosZ * 31) * 31 + RelPosX


  override def equals(that: Any): Boolean = {
    if (!that.isInstanceOf[UnifiedPos]) return false
    //Shit's broke yo
    def other = that.asInstanceOf[UnifiedPos]

    if (hashCode == other.hashCode)
      true
    else
      false

  }

}
