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
object ShipEntity {
  var nextID: ThreadLocal[Int] = new ThreadLocal[Int]()
  nextID.set(0)
}
class ShipEntity(pos: BlockPos, world: World, blockSet: Set[BlockPos], shipBlockPos: BlockPos) extends Entity(world) {

  def this(world: World) = this(new BlockPos(0, 0, 0), world, Set[BlockPos](), new BlockPos(0, 0, 0))


  posX = pos.getX
  posY = pos.getY
  posZ = pos.getZ

  var ShipBlockPos: BlockPos = shipBlockPos
  val ShipWorld: ShipWorld = new ShipWorld(world, shipBlockPos, blockSet, this)
  val ShipID = if (blockSet.nonEmpty) ShipEntity.nextID.get else -1

  if (blockSet.nonEmpty) ShipEntity.nextID.set(ShipEntity.nextID.get + 1)


  val InteractionHandler: ShipInteractionHandler = new ShipInteractionHandler
  private var boundingBox = ShipWorld.genBoundingBox()
  private var relativeBoundingBox = ShipWorld.genRelativeBoundingBox()


  override def getEntityBoundingBox = if (ShipWorld.BlockSet.nonEmpty) boundingBox else new AxisAlignedBB(0, 0, 0, 0, 0, 0)

  def getRelativeBoundingBox = relativeBoundingBox


  override def writeEntityToNBT(tagCompound: NBTTagCompound): Unit = {

  }

  override def readEntityFromNBT(tagCompund: NBTTagCompound): Unit = {

  }

  override def entityInit(): Unit = {
  }
  override def onUpdate() = {
    if (!ShipWorld.isValid) {
      this.setDead()
    }
    setPositionAndRotation(posX, posY, posZ, rotationYaw + 1f, rotationPitch + 0.1f)
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

  override def setPositionAndRotation(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) = {
    setPosition(x, y, z)
    rotationYaw = yaw
    rotationPitch = pitch
  }


  override def canBeCollidedWith = true

  override def interactAt(player: EntityPlayer, target: Vec3) = InteractionHandler.interactionFired(player, target)




}
