package mrpf1ster.flyingships.entities

import javax.vecmath.Quat4f

import mrpf1ster.flyingships.blocks.ShipCreatorBlock
import mrpf1ster.flyingships.network.ClientSpawnShipHandler
import mrpf1ster.flyingships.util.{BoundingBox, UnifiedPos}
import mrpf1ster.flyingships.world.{ShipWorld, ShipWorldClient, ShipWorldServer}
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util._
import net.minecraft.world.World

import scala.collection.mutable
import scala.collection.mutable.{Set => mSet}
import scala.reflect.io.Path
/**
  * Created by EJ on 2/21/2016.
  */
class EntityShip(pos: BlockPos, world: World, blockSet: Set[BlockPos]) extends Entity(world) {

  // Temp constructor because I haven't implemented entity saving yet
  def this(world: World) = this(new BlockPos(0, 0, 0), world, Set[BlockPos]())
  // Set position
  posX = pos.getX
  posY = pos.getY
  posZ = pos.getZ
  setPosition(posX,posY,posZ)

  // Fake world that holds all the blocks on the ship
  var Shipworld: ShipWorld = null

  // Handles interacting with the ship, (left and right clicking on blocks on the ship)
  // Relevant only on client
  var InteractionHandler: ShipInteractionHandler = new ShipInteractionHandler(Shipworld)

  // Rotation of the ship in Quaternions
  private var Rotation: Quat4f = new Quat4f(0, 0, 0, 1f)

  var oldRotation: Quat4f = Rotation

  // Returns ship direction based on which way the creator block is facing
  def ShipDirection: EnumFacing = if (Shipworld != null && Shipworld.isShipValid) Shipworld.ShipBlock.getValue(ShipCreatorBlock.FACING) else null


  private var _boundingBox:BoundingBox = null

  def getBoundingBox = _boundingBox

  generateBoundingBox()

  def generateBoundingBox() = {
    if (Shipworld != null)
      _boundingBox = new BoundingBox(BoundingBox.generateRotated(Shipworld.BlocksOnShip.toSet, Rotation), BoundingBox.generateRotatedRelative(Shipworld.BlocksOnShip.toSet, Rotation), Rotation, getPositionVector)
  }

  // Returns ship creator block for the ship
  def ShipBlock = Shipworld.ShipBlock


  override def getEntityBoundingBox = if (Shipworld != null && Shipworld.isShipValid && _boundingBox != null) _boundingBox.AABB else new AxisAlignedBB(0, 0, 0, 0, 0, 0)

  def createShipWorld() = {
    Shipworld = if (worldObj.isRemote) new ShipWorldClient(worldObj, this) else new ShipWorldServer(worldObj, this, getUniqueID)
    InteractionHandler = new ShipInteractionHandler(Shipworld)
  }
  override def writeEntityToNBT(tagCompound: NBTTagCompound): Unit = {
    // Quaternion Rotation
    updateRotationFromServer()
    tagCompound.setFloat("RotX", Rotation.getX)
    tagCompound.setFloat("RotY", Rotation.getY)
    tagCompound.setFloat("RotZ", Rotation.getZ)
    tagCompound.setFloat("RotW", Rotation.getW)

    val blocksOnShipX = Shipworld.BlocksOnShip.toArray.map(pos => pos.RelPosX)
    val blocksOnShipY = Shipworld.BlocksOnShip.toArray.map(pos => pos.RelPosY)
    val blocksOnShipZ = Shipworld.BlocksOnShip.toArray.map(pos => pos.RelPosZ)

    // BlocksOnShip
    tagCompound.setIntArray("BlocksOnShipX", blocksOnShipX)
    tagCompound.setIntArray("BlocksOnShipY", blocksOnShipY)
    tagCompound.setIntArray("BlocksOnShipZ", blocksOnShipZ)

    // The Shipworld itself is saved to the world file
    Shipworld.getChunkProvider.saveChunks(true, null)
  }

  override def readEntityFromNBT(tagCompound: NBTTagCompound): Unit = {
    Rotation = new Quat4f(tagCompound.getFloat("RotX"), tagCompound.getFloat("RotY"), tagCompound.getFloat("RotZ"), tagCompound.getFloat("RotW"))
    createShipWorld()
    val blocksOnShipX = tagCompound.getIntArray("BlocksOnShipX")
    val blocksOnShipY = tagCompound.getIntArray("BlocksOnShipY")
    val blocksOnShipZ = tagCompound.getIntArray("BlocksOnShipZ")

    val blocksOnShip = (0 until blocksOnShipX.size).map(i => new UnifiedPos(new BlockPos(blocksOnShipX(i), blocksOnShipY(i), blocksOnShipZ(i)), Shipworld.OriginPos, true))
    // Check to make sure blocks aren't air
    Shipworld.BlocksOnShip = mutable.Set(blocksOnShip: _*).filter(pos => Shipworld.getBlockState(pos.RelativePos).getBlock != Blocks.air)

  }
  override def entityInit(): Unit = {
    dataWatcher.addObject[Float](5, 0.0f)
    dataWatcher.addObject[Float](6, 0.0f)
    dataWatcher.addObject[Float](7, 0.0f)
    dataWatcher.addObject[Float](8, 1.0f)

  }

  override def setDead() = {
    super.setDead()
    if (Shipworld != null && !Shipworld.isRemote) {
      //Todo: Think about backing up shipworlds
      // Flush everything to the disk...
      Shipworld.getSaveHandler.flush()
      // and delete recursively the world path
      Path(Shipworld.getSaveHandler.getWorldDirectory.getPath).deleteRecursively()
    }

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
    if (Shipworld.isRemote) return
    oldRotation = Rotation
    Rotation = newRotation
    dataWatcher.updateObject(5, newRotation.getX)
    dataWatcher.updateObject(6, newRotation.getY)
    dataWatcher.updateObject(7, newRotation.getZ)
    dataWatcher.updateObject(8, newRotation.getW)
  }

  def getRotation: Quat4f = Rotation

  override def onUpdate(): Unit = {
    val hasSpawnListing = ClientSpawnShipHandler.spawnMap.contains(getEntityId)
    if (Shipworld == null) {
      if (hasSpawnListing) ClientSpawnShipHandler.requestShipworld(getEntityId)
      return
    }

    // If the Ship is empty and there's no spawn entry for it, delete it
    if (!Shipworld.isShipValid && !hasSpawnListing) {
      this.setDead()
    }
    else if (hasSpawnListing) {
      ClientSpawnShipHandler.requestShipworld(getEntityId)
    }

    if (Shipworld.isRemote)
      updateRotationFromServer()
    else {
      //debugDoRotate()
      setRotation(new Quat4f(0, 0, 0, 1f))
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
    Shipworld.onShipMove()

  }


  override def moveEntity(x: Double, y:Double, z:Double) = {
    setPosition(posX + x,posY + y,posZ + z)
  }
  override def canBeCollidedWith: Boolean = true

  override def canBePushed: Boolean = false

  override def applyEntityCollision(entityIn: Entity): Unit = {}

  override def setEntityBoundingBox(bb: AxisAlignedBB): Unit = {}

  override def getDistanceSqToEntity(entityIn: Entity): Double = {
    val clampedX = MathHelper.clamp_double(entityIn.posX, _boundingBox.MinPos.xCoord, _boundingBox.MaxPos.xCoord)
    val clampedY = MathHelper.clamp_double(entityIn.posY, _boundingBox.MinPos.yCoord, _boundingBox.MaxPos.yCoord)
    val clampedZ = MathHelper.clamp_double(entityIn.posZ, _boundingBox.MinPos.zCoord, _boundingBox.MaxPos.zCoord)

    val d0: Double = clampedX - entityIn.posX
    val d1: Double = clampedY - entityIn.posY
    val d2: Double = clampedZ - entityIn.posZ
    d0 * d0 + d1 * d1 + d2 * d2
  }
}
