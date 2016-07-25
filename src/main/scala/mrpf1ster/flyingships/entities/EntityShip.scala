package mrpf1ster.flyingships.entities

import java.util.UUID
import javax.vecmath.Quat4f

import com.google.common.base.Predicates
import mrpf1ster.flyingships.blocks.ShipCreatorBlock
import mrpf1ster.flyingships.network.ClientSpawnShipHandler
import mrpf1ster.flyingships.util.{BoundingBox, UnifiedPos}
import mrpf1ster.flyingships.world.{ShipWorld, ShipWorldClient, ShipWorldServer}
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util._
import net.minecraft.world.World

import scala.collection.mutable.{Set => mSet}
/**
  * Created by EJ on 2/21/2016.
  */
object EntityShip {

  def LocateShip(ShipUUID: UUID): Option[EntityShip] = {
    val thePlayer = Minecraft.getMinecraft.thePlayer

    def world = thePlayer.getEntityWorld

    val entities = world.getEntities(classOf[EntityShip], Predicates.alwaysTrue[EntityShip])
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
class EntityShip(pos: BlockPos, world: World, blockSet: Set[BlockPos]) extends Entity(world) {

  // Temp constructor because I haven't implemented entity saving yet
  def this(world: World) = this(new BlockPos(0, 0, 0), world, Set[BlockPos]())

  // Set position
  posX = pos.getX
  posY = pos.getY
  posZ = pos.getZ
  setPosition(posX,posY,posZ)

  // Fake world that holds all the blocks on the ship
  val ShipWorld: ShipWorld = if (worldObj.isRemote) new ShipWorldClient(worldObj, this) else new ShipWorldServer(worldObj, this)
  ShipWorld.moveBlocks(blockSet.map(UnifiedPos(_, ShipWorld.OriginPos, IsRelative = false)))

  // Handles interacting with the ship, (left and right clicking on blocks on the ship)
  // Relevant only on client
  val InteractionHandler: ShipInteractionHandler = new ShipInteractionHandler(ShipWorld)

  // Rotation of the ship in Quaternions
  private var Rotation: Quat4f = new Quat4f(0, 0, 0, 1f)

  var oldRotation: Quat4f = Rotation

  // Returns ship direction based on which way the creator block is facing
  def ShipDirection: EnumFacing = if (ShipWorld != null && ShipWorld.isValid) ShipWorld.ShipBlock.getValue(ShipCreatorBlock.FACING) else null


  private var _boundingBox:BoundingBox = null

  generateBoundingBox()

  def getBoundingBox: BoundingBox = _boundingBox

  def generateBoundingBox() = {
    if (ShipWorld != null)
      _boundingBox = new BoundingBox(BoundingBox.generateRotated(ShipWorld.BlocksOnShip.toSet, Rotation), BoundingBox.generateRotatedRelative(ShipWorld.BlocksOnShip.toSet, Rotation), Rotation, getPositionVector)
  }

  // Returns ship creator block for the ship
  def ShipBlock = ShipWorld.ShipBlock


  override def getEntityBoundingBox = if (ShipWorld != null && ShipWorld.isValid && _boundingBox != null) _boundingBox.AABB else new AxisAlignedBB(0, 0, 0, 0, 0, 0)


  override def writeEntityToNBT(tagCompound: NBTTagCompound): Unit = {

  }

  override def readEntityFromNBT(tagCompound: NBTTagCompound): Unit = {

  }
  override def entityInit(): Unit = {
    dataWatcher.addObject[Float](5, 0.0f)
    dataWatcher.addObject[Float](6, 0.0f)
    dataWatcher.addObject[Float](7, 0.0f)
    dataWatcher.addObject[Float](8, 1.0f)

  }

  private def debugDoRotate() = {
    val deg15 = new Quat4f(0, 0, 0.94f, 0.94f)
    deg15.mul(Rotation, deg15)
    val newRot = Rotation.clone().asInstanceOf[Quat4f]
    newRot.interpolate(deg15, 0.01f)
    setRotation(newRot)
  }

  def updateRotationFromServer(): Unit = {
    oldRotation = Rotation
    Rotation = new Quat4f(dataWatcher.getWatchableObjectFloat(5), dataWatcher.getWatchableObjectFloat(6), dataWatcher.getWatchableObjectFloat(7), dataWatcher.getWatchableObjectFloat(8))
  }

  def setRotation(newRotation: Quat4f): Unit = {
    if (ShipWorld.isRemote) return
    oldRotation = Rotation
    Rotation = newRotation
    dataWatcher.updateObject(5, newRotation.getX)
    dataWatcher.updateObject(6, newRotation.getY)
    dataWatcher.updateObject(7, newRotation.getZ)
    dataWatcher.updateObject(8, newRotation.getW)
  }

  def getRotation: Quat4f = Rotation

  override def onUpdate(): Unit = {
    if (ShipWorld == null) return
    // If the Ship is empty and theres no spawn entry for it, delete it
    val hasSpawnListing = ClientSpawnShipHandler.spawnQueue.contains(getEntityId)
    if (!ShipWorld.isValid && !hasSpawnListing) {
      this.setDead()
    }
    else if (hasSpawnListing) {
      ClientSpawnShipHandler.onShipSpawn(getEntityId)
    }

    if (ShipWorld.isRemote)
      updateRotationFromServer()
    else {
      debugDoRotate()
      moveEntity(motionX, motionY, motionZ)
    }


    if (_boundingBox != null)
      _boundingBox = _boundingBox.moveTo(getPositionVector).rotateTo(Rotation)


  }

  override def setPosition(x: Double, y: Double, z: Double):Unit = {
    if (posX == x && posY == y && posZ == z) return
    // Update positions
    prevPosX = posX
    prevPosY = posY
    prevPosZ = posZ
    posX = x
    posY = y
    posZ = z
    ShipWorld.onShipMove()

  }


  override def moveEntity(x: Double, y:Double, z:Double) = {
    setPosition(posX + x,posY + y,posZ + z)
  }
  override def canBeCollidedWith: Boolean = true

  override def canBePushed: Boolean = false

  // Right Click
  override def interactFirst(player: EntityPlayer): Boolean = if (ShipWorld.isRemote) InteractionHandler.onShipRightClick(player) else false




}
