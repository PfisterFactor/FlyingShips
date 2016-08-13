package mrpf1ster.flyingships.world

import java.util
import java.util.UUID

import com.google.common.base.Predicate
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.util.{ShipLocator, UnifiedPos, UnifiedVec, VectorUtils}
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util._
import net.minecraft.world.chunk.{Chunk, IChunkProvider}
import net.minecraft.world.{ChunkCoordIntPair, World}

import scala.collection.JavaConversions._
import scala.collection.mutable.{Map => mMap, Set => mSet}


/**
  * Created by EJ on 3/2/2016.
  */
object ShipWorld {
  // The Ship Block's Position, y is 128 to stick with the chunk's height limits
  val ShipBlockPos: BlockPos = new BlockPos(0, 128, 0)
  var ShipMouseOverID: Int = -1
  val DEFUALTMOUSEOVER: MovingObjectPosition = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, new Vec3(0, 0, 0), EnumFacing.UP, new BlockPos(-1, -1, -1))
  var ShipMouseOver: MovingObjectPosition = DEFUALTMOUSEOVER
  val ShipBlockVec: Vec3 = new Vec3(ShipBlockPos)
  var doAccessing = false


  def startAccessing(): Unit = {
    doAccessing = true
  }

  def stopAccessing(world: World): Unit = {
    doAccessing = false
    ShipLocator.getShips(world).foreach(ent => if (ent.Shipworld != null) ent.Shipworld.wasAccessed = false)
  }
}

abstract class ShipWorld(originWorld: World, ship: EntityShip, uUID: UUID) extends DetachedWorld(originWorld, "Ship", uUID) {
  val OriginWorld = originWorld
  val Ship = ship
  // The coordinates of the ship block in the origin world. Conveniently the EntityShip's position
  def OriginPos() = Ship.getPosition

  def OriginVec() = Ship.getPositionVector

  chunkProvider = createChunkProvider()

  var BlocksOnShip: mSet[UnifiedPos] = mSet()
  var ChunksOnShip: mSet[ChunkCoordIntPair] = mSet()

  // TODO: Change this to be the biome directly under the ship
  val BiomeID = OriginWorld.getBiomeGenForCoords(OriginPos()).biomeID

  // The Ship Block
  def ShipBlock = getBlockState(ShipWorld.ShipBlockPos)

  def addBlockToShip(pos: BlockPos) = {
    BlocksOnShip.add(UnifiedPos(pos, OriginPos, IsRelative = true))
    ChunksOnShip.add(new ChunkCoordIntPair(pos.getX >> 4, pos.getZ >> 4))
  }

  def removeBlockFromShip(pos: BlockPos) = {
    BlocksOnShip.remove(UnifiedPos(pos, OriginPos, IsRelative = true))
    ChunksOnShip.remove(new ChunkCoordIntPair(pos.getX >> 4, pos.getZ >> 4))
  }
  // Todo: Optimize and make it more reliable
  def moveBlocks(blockSet: Set[UnifiedPos]): Unit = {
    def blockIsValid(uPos: UnifiedPos): Boolean = {
      val blockstate = OriginWorld.getBlockState(uPos.WorldPos)
      blockstate != null && blockstate.getBlock != Blocks.air
    }
    def blockIsFullCube(uPos: UnifiedPos) = OriginWorld.getBlockState(uPos.WorldPos).getBlock.isFullCube
    // Move blocks onto ship
    // First the solid blocks, then everything else
    // So torches and blocks that depend on other blocks get set correctly
    // Todo: Clean this up and implement a hierarchy for blocks
    val filteredSet = blockSet.filter(blockIsValid)
    val firstBlocks = filteredSet.filter(blockIsFullCube)
    val everythingElse = filteredSet -- firstBlocks

    firstBlocks.foreach(uPos => {
      val bs = OriginWorld.getBlockState(uPos.WorldPos)
      addBlockToShip(uPos.RelativePos)
      setBlockState(uPos.RelativePos, bs, 0)
    })
    everythingElse.foreach(uPos => {
      val bs = OriginWorld.getBlockState(uPos.WorldPos)
      addBlockToShip(uPos.RelativePos)
      setBlockState(uPos.RelativePos, bs, 0)
    })

    if (!this.isShipValid) return
    // Move tile entities onto ship
    BlocksOnShip
      .map(uPos => (uPos, OriginWorld.getTileEntity(uPos.WorldPos)))
      .filter(_._2 != null)
      .foreach(pair => {
        try {
          val nbt = new NBTTagCompound
          pair._2.writeToNBT(nbt)
          // Check if a tile entity was already created
          val teOnShip = getTileEntity(pair._1.RelativePos)
          if (teOnShip != null) {
            teOnShip.readFromNBT(nbt)
            teOnShip.validate()
            teOnShip.setWorldObj(this)
            teOnShip.setPos(pair._1.RelativePos)
          }
          else {
            val copyTileEntity = TileEntity.createAndLoadEntity(nbt)
            copyTileEntity.setWorldObj(this)
            copyTileEntity.setPos(pair._1.RelativePos)
            copyTileEntity.validate()
            addTileEntity(copyTileEntity)
          }

        }
        catch {
          case ex: Exception => println(s"There was an error moving TileEntity ${pair._2.getClass.getName} at ${pair._1.WorldPos}\n$ex") // Error reporting
        }
      })
    if (!isRemote)
      this.asInstanceOf[ShipWorldServer].createPlayerManager()
  }

  override def createChunkProvider(): IChunkProvider

  override def getProviderName: String = chunkProvider.makeString()

  override def checkNoEntityCollision(aabb: AxisAlignedBB): Boolean = checkNoEntityCollision(aabb, null)

  override def checkNoEntityCollision(aabb: AxisAlignedBB, entity: Entity): Boolean = {
    // Todo: Implement this
    true
  }

  // Fix for entities on ship later
  // Also do not touch these
  // ...
  // Ever
  // Java generics should not mix with Scala generics, christ.
  override def getEntitiesWithinAABB[T <: Entity](classEntity: java.lang.Class[_ <: T], bb: AxisAlignedBB): java.util.List[T] = getEntitiesWithinAABB[T](classEntity, bb, EntitySelectors.NOT_SPECTATING)

  override def getEntitiesWithinAABBExcludingEntity(entityIn: Entity, bb: AxisAlignedBB): util.List[Entity] = OriginWorld.getEntitiesWithinAABBExcludingEntity(entityIn, bb.offset(OriginVec().xCoord, OriginVec().yCoord, OriginVec().zCoord)).filterNot(ent => ent.isInstanceOf[EntityShip])

  override def getEntitiesWithinAABB[T <: Entity](clazz: java.lang.Class[_ <: T], aabb: AxisAlignedBB, filter: Predicate[_ >: T]): util.List[T] = OriginWorld.getEntitiesWithinAABB[T](clazz, aabb.offset(OriginVec().xCoord, OriginVec().yCoord, OriginVec().zCoord), filter).filterNot(ent => ent.isInstanceOf[EntityShip])

  override def getEntitiesInAABBexcluding(entityIn: Entity, boundingBox: AxisAlignedBB, predicate: Predicate[_ >: Entity]): java.util.List[Entity] = OriginWorld.getEntitiesInAABBexcluding(entityIn, boundingBox.offset(OriginVec().xCoord, OriginVec().yCoord, OriginVec().zCoord), predicate).filterNot(ent => ent.isInstanceOf[EntityShip])

  override def tick(): Unit

  override def spawnEntityInWorld(entity: Entity): Boolean = {
    val worldPos = UnifiedVec.convertToWorld(VectorUtils.rotatePointToShip(entity.getPositionVector, Ship), Ship.getPositionVector)
    entity.setPosition(worldPos.xCoord, worldPos.yCoord, worldPos.zCoord)
    entity.setWorld(OriginWorld)

    OriginWorld.spawnEntityInWorld(entity)
  }

  // Hackish way to get the tile entity the player is interacting with in the onPlayerContainerOpen event
  var wasAccessed = false

  private def accessingHack(any: Any): Any = {
    if (ShipWorld.doAccessing)
      wasAccessed = any != null
    else
      wasAccessed = false
  }


  override def setBlockState(pos: BlockPos, newState: IBlockState, flags: Int): Boolean = {
    if (!isValid(pos)) return false

    val uPos = UnifiedPos(pos, OriginPos, IsRelative = true)
    val contains = BlocksOnShip.contains(uPos)
    addBlockToShip(uPos.RelativePos)
    if (!contains && newState.getBlock != Blocks.air) {
      Ship.generateBoundingBox()
    }
    else if (newState.getBlock == Blocks.air) {
      removeBlockFromShip(uPos.RelativePos)
      Ship.generateBoundingBox()
    }

    val chunk: Chunk = this.getChunkFromBlockCoords(pos)
    val block: Block = newState.getBlock
    val oldBlock: Block = getBlockState(pos).getBlock
    val oldLight: Int = oldBlock.getLightValue(this, pos)
    val oldOpacity: Int = oldBlock.getLightOpacity(this, pos)
    val iblockstate: IBlockState = chunk.setBlockState(pos, newState)

    if (iblockstate == null) return false

    if (block.getLightOpacity(this, pos) != oldOpacity || block.getLightValue(this, pos) != oldLight)
      this.checkLight(pos)

    this.markAndNotifyBlock(pos, chunk, iblockstate, newState, flags)
    true
  }

  override def markAndNotifyBlock(pos: BlockPos, chunk: Chunk, iblockstate: IBlockState, newState: IBlockState, flags: Int) = {
    if ((flags & 2) != 0 && (!this.isRemote || (flags & 4) == 0)) {
      this.markBlockForUpdate(pos)
    }
    if (!this.isRemote && (flags & 1) != 0) {
      this.notifyNeighborsRespectDebug(pos, newState.getBlock)
      if (newState.getBlock.hasComparatorInputOverride) {
        this.updateComparatorOutputLevel(pos, newState.getBlock)
      }
    }

  }

  override def getBlockState(pos: BlockPos): IBlockState = {
    val state = super.getBlockState(pos)
    accessingHack(state)
    state
  }
  override def getTileEntity(pos: BlockPos) = {
    val te = super.getTileEntity(pos)
    accessingHack(te)
    te
  }

  def onChunkLoad(x: Int, z: Int): Unit

  def onChunkUnload(x: Int, z: Int): Unit

  def getClosestBlockPosToPlayer(entityPlayer: EntityPlayer): BlockPos = BlocksOnShip.minBy(upos => entityPlayer.getDistanceSq(upos.WorldPos)).WorldPos

  def getClosestBlockPosToPlayerXZ(entityPlayer: EntityPlayer): BlockPos = BlocksOnShip.minBy(upos => Math.pow(upos.WorldPosX - entityPlayer.posX, 2) + Math.pow(upos.WorldPosZ - entityPlayer.posZ, 2)).RelativePos

  // Assumes coordinates are relative to the ship
  override def getClosestPlayer(x: Double, y: Double, z: Double, distance: Double): EntityPlayer = {
    val worldVec = UnifiedVec.convertToWorld(x, y, z, OriginVec())

    val players = OriginWorld.playerEntities.filter(p => !p.isSpectator && p.getDistanceSq(worldVec.xCoord, worldVec.yCoord, worldVec.zCoord) <= distance * distance)
    var playerEntity: Option[EntityPlayer] = None

    if (players.nonEmpty)
      playerEntity = Some(players.minBy(_.getDistanceSq(worldVec.xCoord, worldVec.yCoord, worldVec.zCoord)))

    if (playerEntity.isDefined)
      PlayerRelative(playerEntity.get, this)
    else
      null
  }


  def addBlockEvent(pos: BlockPos, block: Block, eventID: Int, eventParam: Int): Unit


  // Ray traces blocks on ship, arguments are non-relative
  // It rotates the look and ray vector against the ship's current rotation so we can use Minecraft's built in world block ray-trace
  override def rayTraceBlocks(start: Vec3, end: Vec3): MovingObjectPosition = {

    val relativeStart = UnifiedVec.convertToRelative(start, Ship.getPositionVector)
    val relativeEnd = UnifiedVec.convertToRelative(end, Ship.getPositionVector)

    // The eye position and ray, rotated by the ship block's center, to the opposite of the ships current rotation
    // We use the interpolated rotation if its on client so the raytrace matches what the client sees
    val rotatedStart = VectorUtils.rotatePointFromShip(relativeStart, Ship, useInterpolated = isRemote)
    val rotatedEnd = VectorUtils.rotatePointFromShip(relativeEnd, Ship, useInterpolated = isRemote)

    // The result of the ray-trace on the ship world
    super.rayTraceBlocks(rotatedStart, rotatedEnd)
  }

  def isShipValid = BlocksOnShip.nonEmpty

  protected def isValid(pos: BlockPos) = pos.getX >= -30000000 && pos.getZ >= -30000000 && pos.getX < 30000000 && pos.getZ < 30000000 && pos.getY >= 0 && pos.getY < 256

  override def playSoundEffect(x: Double, y: Double, z: Double, soundName: String, volume: Float, pitch: Float) = {
    val newVec = UnifiedVec.convertToWorld(new Vec3(x, y, z), Ship.getPositionVector)
    OriginWorld.playSoundEffect(newVec.xCoord, newVec.yCoord, newVec.zCoord, soundName, volume, pitch)
  }


  override def isSideSolid(pos: BlockPos, side: EnumFacing, default: Boolean) = getBlockState(pos).getBlock.isSideSolid(this, pos, side)

  override def getBiomeGenForCoords(pos: BlockPos) = OriginWorld.getBiomeGenForCoords(Ship.getPosition.add(pos))

  override def getBiomeGenForCoordsBody(pos: BlockPos) = OriginWorld.getBiomeGenForCoords(Ship.getPosition.add(pos))


}
