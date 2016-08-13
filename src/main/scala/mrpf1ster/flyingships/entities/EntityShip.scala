package mrpf1ster.flyingships.entities

import java.util.UUID
import javax.vecmath.Quat4f

import mrpf1ster.flyingships.blocks.ShipCreatorBlock
import mrpf1ster.flyingships.util.{BoundingBox, UnifiedPos}
import mrpf1ster.flyingships.world.{ShipWorld, ShipWorldClient, ShipWorldServer}
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util._
import net.minecraft.world.{ChunkCoordIntPair, World}

import scala.collection.mutable
import scala.collection.mutable.{Set => mSet}
import scala.reflect.io.Path
/**
  * Created by EJ on 2/21/2016.
  */
object EntityShip {
  private var nextShipID = 0

  def getNextShipID(): Int = {
    nextShipID += 1
    nextShipID - 1
  }

  def maxShipID(id: Int): Unit = {
    nextShipID = Math.max(nextShipID, id)
  }

  def resetIDs(): Unit = nextShipID = 0

  def addShipToWorld(entityShip: EntityShip): Boolean = {
    if (entityShip == null || entityShip.Shipworld == null || entityShip.Shipworld.OriginWorld.isRemote) return false
    EntityShipTracker.trackShip(entityShip)
    true
  }
}

class EntityShip(pos: BlockPos, world: World) extends Entity(world) {

  def this(world: World) = this(new BlockPos(0, 0, 0), world)

  setWorld(world)
  // Set position
  posX = pos.getX
  posY = pos.getY
  posZ = pos.getZ
  setPosition(posX,posY,posZ)

  // Our ShipID, independent from EntityID
  private var shipID = -1

  def ShipID = shipID

  def setShipID(id: Int) = shipID = id

  // Fake world that holds all the blocks on the ship
  var Shipworld: ShipWorld = null

  // Handles interacting with the ship, (left and right clicking on blocks on the ship)
  // Relevant only on client
  var InteractionHandler: ShipInteractionHandler = new ShipInteractionHandler(Shipworld)

  // Rotation of the ship in Quaternions
  private var Rotation: Quat4f = new Quat4f(0, 0, 0, 1f)

  var interpolatedRotation: Quat4f = Rotation.clone().asInstanceOf[Quat4f]

  // Returns ship direction based on which way the creator block is facing
  def ShipDirection: EnumFacing = if (Shipworld != null && Shipworld.isShipValid) Shipworld.ShipBlock.getValue(ShipCreatorBlock.FACING) else null


  private var _boundingBox:BoundingBox = null

  def getBoundingBox = _boundingBox

  generateBoundingBox()

  noClip = true

  def generateBoundingBox() = {
    if (Shipworld != null)
      _boundingBox = new BoundingBox(BoundingBox.generateRotated(Shipworld.BlocksOnShip.toSet, Rotation), BoundingBox.generateRotatedRelative(Shipworld.BlocksOnShip.toSet, Rotation), Rotation, getPositionVector)
  }

  // Returns ship creator block for the ship
  def ShipBlock = Shipworld.ShipBlock

  override def getEntityId = {
    throw new IllegalAccessException("Don't use entity id! Use ShipID instead!")
  }
  override def getEntityBoundingBox = if (Shipworld != null && Shipworld.isShipValid && _boundingBox != null) _boundingBox.AABB else new AxisAlignedBB(0, 0, 0, 0, 0, 0)

  def createShipWorld() = {
    Shipworld = if (worldObj.isRemote) new ShipWorldClient(worldObj, this) else new ShipWorldServer(worldObj, this, entityUniqueID)
    InteractionHandler = new ShipInteractionHandler(Shipworld)
  }
  override def writeEntityToNBT(tagCompound: NBTTagCompound): Unit = {
    // ShipID
    tagCompound.setInteger("ShipID", shipID)

    // Position
    tagCompound.setDouble("PosX", posX)
    tagCompound.setDouble("PosY", posY)
    tagCompound.setDouble("PosZ", posZ)

    // Quaternion Rotation
    tagCompound.setFloat("RotX", Rotation.getX)
    tagCompound.setFloat("RotY", Rotation.getY)
    tagCompound.setFloat("RotZ", Rotation.getZ)
    tagCompound.setFloat("RotW", Rotation.getW)

    // ShipUUID
    tagCompound.setLong("LeastUUID", Shipworld.UUID.getLeastSignificantBits)
    tagCompound.setLong("MostUUID", Shipworld.UUID.getMostSignificantBits)

    // Only write non-air blocks
    Shipworld.BlocksOnShip = Shipworld.BlocksOnShip.filter(pos => Shipworld.getBlockState(pos.RelativePos) != Blocks.air.getDefaultState)
    val blocksOnShipX = Shipworld.BlocksOnShip.toArray.map(pos => pos.RelPosX)
    val blocksOnShipY = Shipworld.BlocksOnShip.toArray.map(pos => pos.RelPosY)
    val blocksOnShipZ = Shipworld.BlocksOnShip.toArray.map(pos => pos.RelPosZ)

    // BlocksOnShip
    tagCompound.setIntArray("BlocksOnShipX", blocksOnShipX)
    tagCompound.setIntArray("BlocksOnShipY", blocksOnShipY)
    tagCompound.setIntArray("BlocksOnShipZ", blocksOnShipZ)
  }

  override def readEntityFromNBT(tagCompound: NBTTagCompound): Unit = {
    // ShipID
    shipID = tagCompound.getInteger("ShipID")

    // Position
    posX = tagCompound.getDouble("PosX")
    posY = tagCompound.getDouble("PosY")
    posZ = tagCompound.getDouble("PosZ")

    // Quaternion Rotation
    Rotation = new Quat4f(tagCompound.getFloat("RotX"), tagCompound.getFloat("RotY"), tagCompound.getFloat("RotZ"), tagCompound.getFloat("RotW"))

    // ShipUUID
    entityUniqueID = new UUID(tagCompound.getLong("MostUUID"), tagCompound.getLong("LeastUUID"))

    // BlocksOnShip
    createShipWorld()
    val blocksOnShipX = tagCompound.getIntArray("BlocksOnShipX")
    val blocksOnShipY = tagCompound.getIntArray("BlocksOnShipY")
    val blocksOnShipZ = tagCompound.getIntArray("BlocksOnShipZ")

    val blocksOnShip = (0 until blocksOnShipX.size).map(i => new UnifiedPos(new BlockPos(blocksOnShipX(i), blocksOnShipY(i), blocksOnShipZ(i)), Shipworld.OriginPos, true))
    Shipworld.BlocksOnShip = mutable.Set(blocksOnShip: _*)
    Shipworld.BlocksOnShip.foreach(uPos => {
      Shipworld.ChunksOnShip.add(new ChunkCoordIntPair(uPos.RelPosX >> 4, uPos.RelPosZ >> 4))
      Shipworld.getChunkProvider.provideChunk(uPos.RelativePos)
    })

    Shipworld.asInstanceOf[ShipWorldServer].createPlayerManager()

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
    newRot.interpolate(deg15, 0.1f)
    setRotation(newRot)
  }

  // Unused on Server
  private var framesBetweenLastServerSync = 0
  // Unused on Server
  private var frameCounter = 0

  // Used in rendering
  // For interpolating between the client rotation to server rotation
  def FramesBetweenLastServerSync = framesBetweenLastServerSync

  // See above
  def IncrementFrameCounter() = frameCounter += 1


  def setRotation(newRotation: Quat4f): Unit = {
    Rotation.set(newRotation)
  }

  def setInterpolatedRotation(newRotation: Quat4f): Unit = {
    interpolatedRotation = newRotation
  }

  def setRotationFromServer(newRotation: Quat4f) = {
    framesBetweenLastServerSync = frameCounter
    frameCounter = 0
    setRotation(newRotation)
  }

  def getRotation: Quat4f = Rotation.clone().asInstanceOf[Quat4f]

  def getInverseRotation: Quat4f = {
    val result = new Quat4f(0, 0, 0, 1f)
    result.inverse(Rotation)
    result
  }

  var clientTicks = 0
  var serverTicks = 0
  override def onUpdate(): Unit = {
    // If the Ship is empty and there's no spawn entry for it, delete it
    if (Shipworld == null || !Shipworld.isShipValid) {
      this.setDead()
    }
    if (!Shipworld.isRemote) {
      //debugDoRotate()
      //setRotation(new Quat4f(0, 0, 0, 1f))
      setVelocity(0.1f, 0f, 0f)
    }

    moveEntity(motionX, motionY, motionZ)


    if (_boundingBox != null)
      _boundingBox = _boundingBox.moveTo(getPositionVector).rotateTo(Rotation)


  }


  override def setVelocity(x: Double, y: Double, z: Double): Unit = {
    velocityChanged = x != motionX || y != motionY || z != motionZ
    motionX = x
    motionY = y
    motionZ = z
  }

  override def setPosition(x: Double, y: Double, z: Double): Unit = {
    if (posX == x && posY == y && posZ == z) return
    // Update positions
    prevPosX = posX
    prevPosY = posY
    prevPosZ = posZ
    posX = x
    posY = y
    posZ = z

  }


  override def moveEntity(x: Double, y:Double, z:Double) = {
    setPosition(posX + x,posY + y,posZ + z)
  }

  override def canBeCollidedWith: Boolean = false

  override def canBePushed: Boolean = false

  override def applyEntityCollision(entityIn: Entity): Unit = {}

  override def setEntityBoundingBox(bb: AxisAlignedBB): Unit = {}

  def shouldRenderInPassOverride(pass: Int) = pass == 0

  def getDistanceSqToShipClamped(entityIn: Entity): Double = {
    val closest = getClosestPoint(entityIn.getPositionVector)
    val d0: Double = closest.xCoord - entityIn.posX
    val d1: Double = closest.yCoord - entityIn.posY
    val d2: Double = closest.zCoord - entityIn.posZ
    d0 * d0 + d1 * d1 + d2 * d2
  }

  def getClosestPoint(vec: Vec3): Vec3 = {
    val clampedX = MathHelper.clamp_double(vec.xCoord, _boundingBox.MinPos.xCoord, _boundingBox.MaxPos.xCoord)
    val clampedY = MathHelper.clamp_double(vec.yCoord, _boundingBox.MinPos.yCoord, _boundingBox.MaxPos.yCoord)
    val clampedZ = MathHelper.clamp_double(vec.zCoord, _boundingBox.MinPos.zCoord, _boundingBox.MaxPos.zCoord)
    new Vec3(clampedX, clampedY, clampedZ)
  }

  override def entityInit(): Unit = {
  }
}
