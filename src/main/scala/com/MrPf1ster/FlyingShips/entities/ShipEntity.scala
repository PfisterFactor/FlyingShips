package com.MrPf1ster.FlyingShips.entities

import javax.vecmath.Quat4f

import com.MrPf1ster.FlyingShips.ShipWorld
import com.MrPf1ster.FlyingShips.blocks.ShipCreatorBlock
import com.MrPf1ster.FlyingShips.util.RotatedBB
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util._
import net.minecraft.world.World
/**
  * Created by EJ on 2/21/2016.
  */
object ShipEntity {
  // This is not right, needs to be sent via packets
  var nextID: ThreadLocal[Int] = new ThreadLocal[Int]()
  nextID.set(0)
}
class ShipEntity(pos: BlockPos, world: World, blockSet: Set[BlockPos], shipBlockPos: BlockPos) extends Entity(world) {

  // Temp constructor because I haven't implemented entity saving yet
  def this(world: World) = this(new BlockPos(0, 0, 0), world, Set[BlockPos](), new BlockPos(0, 0, 0))

  posX = pos.getX
  posY = pos.getY
  posZ = pos.getZ
  // This needs to be synced with the server
  val ShipID = if (blockSet.nonEmpty) ShipEntity.nextID.get else -1
  if (blockSet.nonEmpty) ShipEntity.nextID.set(ShipEntity.nextID.get + 1)

  // Handles interacting with the ship, (left and right clicking on blocks on the ship)
  val InteractionHandler: ShipInteractionHandler = new ShipInteractionHandler

  // Fake world that holds all the blocks on the ship
  val ShipWorld: ShipWorld = new ShipWorld(world, shipBlockPos, blockSet, this)

  // Rotation of the ship in Quaternions
  var Rotation: Quat4f = new Quat4f(0, 0, 1f, 1)

  // Returns ship direction based on which way the creator block is facing
  val ShipDirection: EnumFacing = if (ShipWorld.isValid) ShipWorld.ShipBlock.getValue(ShipCreatorBlock.FACING) else null

  // Returns ship creator block for the ship
  def ShipBlock = ShipWorld.ShipBlock

  // The ship's Axis Aligned bounding box
  private var boundingBox: AxisAlignedBB = ShipWorld.genBoundingBox()

  // The ship's Axis Aligned bounding box relative to the ship's creator block
  private var relativeBoundingBox: AxisAlignedBB = ShipWorld.genRelativeBoundingBox()

  // The ship's Rotated bounding box
  var RotatedBB = new RotatedBB(getEntityBoundingBox, new Vec3(posX, posY, posZ), Rotation)

  // Returns the axis aligned bounding box relative to the world
  override def getEntityBoundingBox = if (ShipWorld.isValid) boundingBox else new AxisAlignedBB(0, 0, 0, 0, 0, 0)


  // Returns an axis aligned bounding box relative to the ship's creator block
  def getRelativeBoundingBox = relativeBoundingBox

  // Returns a rotated bounding box relative to the ship's creator block
  def getRelativeRotatedBoundingBox = {
    new RotatedBB(getRelativeBoundingBox, new Vec3(0.5, 0.5, 0.5), Rotation)
  }

  override def writeEntityToNBT(tagCompound: NBTTagCompound): Unit = {

  }

  override def readEntityFromNBT(tagCompound: NBTTagCompound): Unit = {

  }

  override def entityInit(): Unit = {
  }
  override def onUpdate() = {
    if (!ShipWorld.isValid) {
      this.setDead()
    }
    setPosition(posX, posY, posZ)
    RotatedBB = RotatedBB.moveTo(posX, posY, posZ)
    val b = new Quat4f(0.94f, 0, 0, 0.94f)
    b.mul(Rotation, b)
    Rotation.interpolate(b, 0.2f)

  }

  override def setPositionAndRotation(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) = {
    setPosition(x, y, z)
    prevRotationYaw = rotationYaw
    prevRotationPitch = rotationPitch

    rotationYaw = yaw % 360
    rotationPitch = pitch % 360
    RotatedBB = RotatedBB.rotateTo(Rotation)
  }

  override def setPosition(x: Double, y: Double, z: Double) = {
    if (boundingBox != null) {
      // Get delta...
      val deltaX = x - posX
      val deltaY = y - posY
      val deltaZ = z - posZ

      // ..and offset the bounding box
      boundingBox = boundingBox.offset(deltaX, deltaY, deltaZ)
      RotatedBB = RotatedBB.moveTo(x, y, z)
    }
    // Update positions
    posX = x
    posY = y
    posZ = z

  }

  override def canBeCollidedWith = true

  override def interactAt(player: EntityPlayer, target: Vec3) = InteractionHandler.interactionFired(player, target)




}
