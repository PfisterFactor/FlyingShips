package com.MrPf1ster.FlyingShips.entities

import com.MrPf1ster.FlyingShips.ShipWorld
import com.MrPf1ster.FlyingShips.blocks.ShipCreatorBlock
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.BlockPos
import net.minecraft.world.{MinecraftException, World}
import net.minecraft.entity.Entity

/**
  * Created by EJ on 2/21/2016.
  */

class ShipEntity(pos:BlockPos, world:World,blockSet : Set[BlockPos], shipBlockPos: BlockPos,owner: EntityPlayer) extends Entity(world) {
  val ShipWorld = new ShipWorld(world,pos,blockSet,this)
  val ShipBlockPos: BlockPos = shipBlockPos
  var AABB = //TODO Implement AABB detection

  if (!world.getBlockState(shipBlockPos).getBlock.isInstanceOf[ShipCreatorBlock]) throw new MinecraftException("Ship entity being created with bad ship block location") // If provided ship block pos is not a ship block, throw error

  /*
  override def getEntityBoundingBox = {

  }
  */
  override def writeEntityToNBT(tagCompound: NBTTagCompound): Unit = {

  }

  override def entityInit(): Unit = {
    println("init entity")
  }
  override def onUpdate() = {
    println("Updating entity")
  }

  override def readEntityFromNBT(tagCompund: NBTTagCompound): Unit = {

  }

}
