package com.MrPf1ster.FlyingShips.entities

import java.lang.Math.{max, min}

import com.MrPf1ster.FlyingShips.ShipWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.{AxisAlignedBB, BlockPos, Vec3}
import net.minecraft.world.World
/**
  * Created by EJ on 2/21/2016.
  */

class ShipEntity(pos: BlockPos, world: World, blockSet: Set[BlockPos], shipBlockPos: BlockPos) extends Entity(world) {

  def this(world: World) = this(new BlockPos(0, 0, 0), world, Set[BlockPos](), new BlockPos(0, 0, 0))

  if (blockSet.isEmpty) {
    this.setDead()
  }

  val ShipWorld = new ShipWorld(world,pos,blockSet,this)
  val ShipBlockPos: BlockPos = shipBlockPos
  val InteractionHandler = new ShipInteractionHandler()

  private val AABB = {
    if (this.isDead) {
      new AxisAlignedBB(0, 0, 0, 0, 0, 0)
    }
    else {
      var minX = Int.MaxValue
      var minY = Int.MaxValue
      var minZ = Int.MaxValue
      var maxX = Int.MinValue
      var maxY = Int.MinValue
      var maxZ = Int.MinValue

      ShipWorld.BlockSet.foreach(pos => {
        minX = min(minX, pos.getX)
        minY = min(minY, pos.getY)
        minZ = min(minZ, pos.getZ)

        maxX = max(maxX, pos.getX)
        maxY = max(maxY, pos.getY)
        maxZ = max(maxZ, pos.getZ)


      })

      val minPos = ShipWorld.getWorldPos(new BlockPos(minX, minY, minZ))
      val maxPos = ShipWorld.getWorldPos(new BlockPos(maxX + 1, maxY + 1, maxZ + 1))

      new AxisAlignedBB(minPos, maxPos)
    }
  }

  override def getEntityBoundingBox = AABB

  def getRelativeBoundingBox = {
    val relativeMinPos = ShipWorld.getRelativePos(new BlockPos(AABB.minX, AABB.minY, AABB.minZ))
    val relativeMaxPos = ShipWorld.getRelativePos(new BlockPos(AABB.maxX, AABB.maxY, AABB.maxZ))
    new AxisAlignedBB(relativeMinPos, relativeMaxPos)
  }


  override def writeEntityToNBT(tagCompound: NBTTagCompound): Unit = {

  }

  override def readEntityFromNBT(tagCompund: NBTTagCompound): Unit = {

  }

  override def entityInit(): Unit = {
    println("init entity")

  }
  override def onUpdate() = {

  }

  override def canBeCollidedWith = true

  override def interactAt(player: EntityPlayer, target: Vec3) = InteractionHandler.interactionFired(player, target)




}
