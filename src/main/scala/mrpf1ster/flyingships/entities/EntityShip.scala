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
import scala.reflect.io.Path
/**
  * Created by EJ on 2/21/2016.
  */
// Holds some global variables relevant to all EntityShips
// Such as the nextShipID and that's about it
object EntityShip {
  // Holds the ship ID we give to the next Ship created
  private var nextShipID = 0

  // Increments nextShipID and then returns it subtracted by one
  // Essentially returns nextShipID and then increments it
  def incrementShipID(): Int = {
    nextShipID += 1
    nextShipID - 1
  }

  // Assigns nextShipID to the max between itself and the number passed in
  def maxShipID(id: Int): Unit = {
    nextShipID = Math.max(nextShipID, id)
  }

  // Set it back to zero
  def resetIDs(): Unit = nextShipID = 0

  // Adds a new EntityShip to an EntityShipTracker
  def addShipToWorld(entityShip: EntityShip): Boolean = {
    if (entityShip == null || entityShip.Shipworld == null || entityShip.Shipworld.OriginWorld.isRemote) return false
    EntityShipTracker.trackShip(entityShip)
    true
  }
}

// In theory, this class holds data relating to the ships state within the OriginWorld
// In practice, this class holds data relating to the ships state within the OriginWorld plus some server syncing and rendering variables
class EntityShip(pos: BlockPos, world: World) extends Entity(world) {

  def this(world: World) = this(new BlockPos(0, 0, 0), world)

  // The world passed in is actually the ShipWorld's OriginWorld
  setWorld(world)

  // Set position
  posX = pos.getX
  posY = pos.getY
  posZ = pos.getZ
  setPosition(posX,posY,posZ)

  // Our ShipID, independent from EntityID
  private var shipID = -1

  // Accessor method for shipID
  def ShipID = shipID

  // Setter method for shipID
  def setShipID(id: Int) = shipID = id

  // Fake world that holds all the blocks and chunks on the ship
  var Shipworld: ShipWorld = null

  // Rotation of the ship in Quaternions
  private var rotation: Quat4f = new Quat4f(0, 0, 0, 1f)

  // Rotation of the ship as the renderer sees it
  // Makes stuff smooth looking
  var InterpolatedRotation: Quat4f = rotation.clone().asInstanceOf[Quat4f]

  // Returns ship direction based on which way the creator block is facing
  def ShipDirection: EnumFacing = if (Shipworld != null && Shipworld.isShipValid) Shipworld.ShipBlock.getValue(ShipCreatorBlock.FACING) else null

  // Holds the rotated bounding box and the axis aligned bounding box for the ship
  // Quite possibly the most organized class in this entire project
  private var boundingBox: BoundingBox = null

  // Accessor method for boundingBox
  def getBoundingBox = boundingBox

  // Generates a bounding box, probably not needed at the moment however cause there is no blocks on the ship.
  generateBoundingBox()

  // In case Minecraft finds out that ShipEntity exists and tries to do collision resolution on it
  // We have our own collision engine
  // ... Well we will once I get around to it
  noClip = true

  // Generates a new boundingbox based on the blocks on the ship
  def generateBoundingBox() = {
    if (Shipworld != null)
      boundingBox = new BoundingBox(BoundingBox.generateRotated(Shipworld.BlocksOnShip.toSet, rotation), BoundingBox.generateRotatedRelative(Shipworld.BlocksOnShip.toSet, rotation), rotation, getPositionVector)
  }

  // Returns ship creator block for the ship
  def ShipBlock = Shipworld.ShipBlock

  // Why would you even THINK about using EntityID
  // EntityShip is special, EntityShip uses ShipID.
  override def getEntityId = {
    throw new IllegalAccessException("Don't use entity id! Use ShipID instead!")
  }

  // Gets the minimum constraining AABB of the ship
  override def getEntityBoundingBox = if (Shipworld != null && Shipworld.isShipValid && boundingBox != null) boundingBox.AABB else new AxisAlignedBB(0, 0, 0, 0, 0, 0)

  // Creates a new Shipworld and stores it
  def createShipWorld() = {
    Shipworld = if (worldObj.isRemote) new ShipWorldClient(worldObj, this) else new ShipWorldServer(worldObj, this, entityUniqueID)
  }

  // Writes the ship to an NBT tag compound
  override def writeEntityToNBT(tagCompound: NBTTagCompound): Unit = {
    // ShipID
    tagCompound.setInteger("ShipID", shipID)

    // Position
    tagCompound.setDouble("PosX", posX)
    tagCompound.setDouble("PosY", posY)
    tagCompound.setDouble("PosZ", posZ)

    // Quaternion Rotation
    tagCompound.setFloat("RotX", rotation.getX)
    tagCompound.setFloat("RotY", rotation.getY)
    tagCompound.setFloat("RotZ", rotation.getZ)
    tagCompound.setFloat("RotW", rotation.getW)

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

  // Reads the ship from an NBT tag compound
  override def readEntityFromNBT(tagCompound: NBTTagCompound): Unit = {
    // ShipID
    shipID = tagCompound.getInteger("ShipID")

    // Position
    posX = tagCompound.getDouble("PosX")
    posY = tagCompound.getDouble("PosY")
    posZ = tagCompound.getDouble("PosZ")

    // Quaternion Rotation
    rotation = new Quat4f(tagCompound.getFloat("RotX"), tagCompound.getFloat("RotY"), tagCompound.getFloat("RotZ"), tagCompound.getFloat("RotW"))

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

  // This kills the ship
  // Deletes the shipworld folder if on server
  // No backsies!
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

  // Debug method used to rotate the ship 15 degrees over time on the Z-axis
  private def debugDoRotate() = {
    val deg15 = new Quat4f(0, 0, 0.94f, 0.94f)
    deg15.mul(rotation, deg15)
    val newRot = rotation.clone().asInstanceOf[Quat4f]
    newRot.interpolate(deg15, 0.01f)
    setRotation(newRot)
  }

  // The amount of frames in-between server syncs
  // Unused on Server
  private var framesBetweenLastServerSync = 0

  // Used to calculate framesBetweenLastServerSync
  // Unused on Server
  private var frameCounter = 0

  // Used in rendering
  // For interpolating between the client rotation to server rotation
  def FramesBetweenLastServerSync = framesBetweenLastServerSync

  // See above
  def IncrementFrameCounter() = frameCounter += 1

  // Setter method for rotation
  def setRotation(newRotation: Quat4f): Unit = {
    rotation.set(newRotation)
  }

  // Setter method for InterpolatedRotation
  def setInterpolatedRotation(newRotation: Quat4f): Unit = {
    InterpolatedRotation = newRotation
  }

  // Updates the framesBetweenLastServerSync variable
  // Then sets the rotation
  def setRotationFromServer(newRotation: Quat4f) = {
    framesBetweenLastServerSync = frameCounter
    frameCounter = 0
    setRotation(newRotation)
  }

  // Accessor method for rotation
  def getRotation: Quat4f = rotation.clone().asInstanceOf[Quat4f]

  // Convenience method
  // Gets the inverse of the rotation while avoiding modifying any state
  def getInverseRotation: Quat4f = {
    val result = new Quat4f(0, 0, 0, 1f)
    result.inverse(rotation)
    result
  }

  // Updates the ship per tick
  // Updates position, velocity, and rotation
  override def onUpdate(): Unit = {
    // If the Ship is empty and there's no spawn entry for it, delete it
    if (Shipworld == null || !Shipworld.isShipValid) {
      this.setDead()
    }
    if (!Shipworld.isRemote) {
      debugDoRotate()
      //setRotation(new Quat4f(0, 0, 0, 1f))
      //setVelocity(1.0f, 0f, 0f)
    }

    moveEntity(motionX, motionY, motionZ)


    if (boundingBox != null)
      boundingBox = boundingBox.moveTo(getPositionVector).rotateTo(rotation)


  }

  // Sets the velocity of the ship
  override def setVelocity(x: Double, y: Double, z: Double): Unit = {
    velocityChanged = x != motionX || y != motionY || z != motionZ
    motionX = x
    motionY = y
    motionZ = z
  }

  // Sets the position of the ship
  override def setPosition(x: Double, y: Double, z: Double): Unit = {
    prevPosX = posX
    prevPosY = posY
    prevPosZ = posZ
    posX = x
    posY = y
    posZ = z

  }

  // Moves the entity's position by xyz
  override def moveEntity(x: Double, y:Double, z:Double) = {
    setPosition(posX + x,posY + y,posZ + z)
  }

  // We handle our collisions
  override def canBeCollidedWith: Boolean = false

  // Still handling our collisions..
  override def canBePushed: Boolean = false

  // Christ almighty...
  override def applyEntityCollision(entityIn: Entity): Unit = {}

  // Does nothing because we calculate our own bounding box
  override def setEntityBoundingBox(bb: AxisAlignedBB): Unit = {}

  // Only render in pass 0
  def shouldRenderInPassOverride(pass: Int) = pass == 0

  // Gets distance to the closest point to the entity
  def getDistanceSqToShipClamped(entityIn: Entity): Double = {
    val closest = getClosestPoint(entityIn.getPositionVector)
    val d0: Double = closest.xCoord - entityIn.posX
    val d1: Double = closest.yCoord - entityIn.posY
    val d2: Double = closest.zCoord - entityIn.posZ
    d0 * d0 + d1 * d1 + d2 * d2
  }

  // Gets the closest point to the vector by clamping it to the bounding box
  def getClosestPoint(vec: Vec3): Vec3 = {
    val clampedX = MathHelper.clamp_double(vec.xCoord, boundingBox.MinPos.xCoord, boundingBox.MaxPos.xCoord)
    val clampedY = MathHelper.clamp_double(vec.yCoord, boundingBox.MinPos.yCoord, boundingBox.MaxPos.yCoord)
    val clampedZ = MathHelper.clamp_double(vec.zCoord, boundingBox.MinPos.zCoord, boundingBox.MaxPos.zCoord)
    new Vec3(clampedX, clampedY, clampedZ)
  }

  // We have no init but we must override it to be an entity
  override def entityInit(): Unit = {

  }
}
