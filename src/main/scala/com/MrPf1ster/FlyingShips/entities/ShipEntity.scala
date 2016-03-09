package com.MrPf1ster.FlyingShips.entities

import com.MrPf1ster.FlyingShips.ShipWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util._
import net.minecraft.world.World
/**
  * Created by EJ on 2/21/2016.
  */

class ShipEntity(pos: BlockPos, world: World, blockSet: Set[BlockPos], shipBlockPos: BlockPos) extends Entity(world) {

  def this(world: World) = this(new BlockPos(0, 0, 0), world, Set[BlockPos](), new BlockPos(0, 0, 0))

  if (blockSet.isEmpty) {
    this.setDead()
  }
  posX = pos.getX
  posY = pos.getY
  posZ = pos.getZ

  var ShipBlockPos: BlockPos = shipBlockPos
  val ShipWorld: ShipWorld = new ShipWorld(world, shipBlockPos, blockSet, this)

  val InteractionHandler: ShipInteractionHandler = new ShipInteractionHandler
  private var boundingBox = ShipWorld.genBoundingBox()
  private var relativeBoundingBox = ShipWorld.genRelativeBoundingBox()


  override def getEntityBoundingBox = boundingBox

  def getRelativeBoundingBox = relativeBoundingBox


  override def writeEntityToNBT(tagCompound: NBTTagCompound): Unit = {

  }

  override def readEntityFromNBT(tagCompund: NBTTagCompound): Unit = {

  }

  override def entityInit(): Unit = {
  }
  override def onUpdate() = {
    setPosition(posX + 0.1, posY + 0.1, posZ + 0.1)
  }

  override def setPosition(x: Double, y: Double, z: Double) = {
    if (boundingBox != null) {
      // Get delta...
      val deltaX = x - posX
      val deltaY = y - posY
      val deltaZ = z - posZ

      // ..and offset the bounding box
      boundingBox = boundingBox.offset(deltaX, deltaY, deltaZ)
      ShipBlockPos = ShipBlockPos.add(deltaX, deltaY, deltaZ)
    }
    // Update positions
    posX = x
    posY = y
    posZ = z

  }

  def getWorldPos(relativePos: BlockPos) = relativePos.add(ShipBlockPos)

  def getRelativePos(worldPos: BlockPos) = {
    worldPos.subtract(ShipBlockPos)
  }

  override def canBeCollidedWith = true

  override def interactAt(player: EntityPlayer, target: Vec3) = InteractionHandler.interactionFired(player, target)




}
