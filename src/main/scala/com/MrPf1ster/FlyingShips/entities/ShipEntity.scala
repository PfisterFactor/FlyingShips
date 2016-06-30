package com.MrPf1ster.FlyingShips.entities

import java.util.UUID
import javax.vecmath.Quat4f

import com.MrPf1ster.FlyingShips.ShipWorld
import com.MrPf1ster.FlyingShips.blocks.ShipCreatorBlock
import com.MrPf1ster.FlyingShips.util.BoundingBox
import com.google.common.base.Predicates
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util._
import net.minecraft.world.World
/**
  * Created by EJ on 2/21/2016.
  */
object ShipEntity {

  def LocateShip(ShipUUID: UUID): Option[ShipEntity] = {
    val thePlayer = Minecraft.getMinecraft.thePlayer

    def world = thePlayer.getEntityWorld

    val entities = world.getEntities(classOf[ShipEntity], Predicates.alwaysTrue[ShipEntity])
    val iter = entities.iterator()

    while (iter.hasNext) {
      val next = iter.next()
      val uuid = next.getPersistentID
      if (uuid.compareTo(ShipUUID) == 0)
        return Some(next)
    }
    None


  }
}
class ShipEntity(pos: BlockPos, world: World, blockSet: Set[BlockPos], shipBlockPos: BlockPos) extends Entity(world) {

  // Temp constructor because I haven't implemented entity saving yet
  def this(world: World) = this(new BlockPos(0, 0, 0), world, Set[BlockPos](), new BlockPos(0, 0, 0))

  // Set position
  posX = pos.getX
  posY = pos.getY
  posZ = pos.getZ


  // Fake world that holds all the blocks on the ship
  val ShipWorld: ShipWorld = new ShipWorld(world, shipBlockPos, blockSet, this)

  // Handles interacting with the ship, (left and right clicking on blocks on the ship)
  val InteractionHandler: ShipInteractionHandler = new ShipInteractionHandler(ShipWorld)

  // Rotation of the ship in Quaternions
  var Rotation: Quat4f = new Quat4f(0f, 0f, 0f, 1f)

  // Returns ship direction based on which way the creator block is facing
  val ShipDirection: EnumFacing = if (ShipWorld.isValid) ShipWorld.ShipBlock.getValue(ShipCreatorBlock.FACING) else null

  private var _boundingBox = new BoundingBox(BoundingBox.generateRotated(ShipWorld.BlockSet, Rotation), BoundingBox.generateRotatedRelative(ShipWorld.BlockSet, Rotation), Rotation, getPositionVector)

  def getBoundingBox: BoundingBox = _boundingBox

  // Returns ship creator block for the ship
  def ShipBlock = ShipWorld.ShipBlock


  override def getEntityBoundingBox = if (ShipWorld.isValid) _boundingBox.AABB else new AxisAlignedBB(0, 0, 0, 0, 0, 0)


  override def writeEntityToNBT(tagCompound: NBTTagCompound): Unit = {

  }

  override def readEntityFromNBT(tagCompound: NBTTagCompound): Unit = {

  }

  override def entityInit(): Unit = {
  }
  override def onUpdate() = {
    // If the Ship is empty
    if (!ShipWorld.isValid) {
      this.setDead()
    }

    //val deg15 = new Quat4f(0.94f, 0, 0, 0.94f)
    //deg15.mul(Rotation, deg15)
    //Rotation.interpolate(deg15, 0.01f)//
    //Rotation = new Quat4f(0, 0, 0, 1)
    _boundingBox = _boundingBox.rotateTo(Rotation)

  }

  override def setPosition(x: Double, y: Double, z: Double) = {
    // Update positions
    posX = x
    posY = y
    posZ = z
    if (_boundingBox != null)
      _boundingBox.moveTo(x, y, z)

  }

  override def canBeCollidedWith = true

  override def interactFirst(player:EntityPlayer) = InteractionHandler.interactionFired(player)




}
