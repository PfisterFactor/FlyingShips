package mrpf1ster.flyingships.world

import java.util
import java.util.{Random, UUID}

import com.google.common.base.Predicate
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.util.{ShipLocator, UnifiedPos, UnifiedVec, VectorUtils}
import mrpf1ster.flyingships.world.chunk.ChunkProviderShip
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util._
import net.minecraft.world.chunk.IChunkProvider
import net.minecraft.world.{World, WorldSettings}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

import scala.collection.JavaConversions._
import scala.collection.mutable.{Map => mMap, Set => mSet}


/**
  * Created by EJ on 3/2/2016.
  */
object ShipWorld {
  // The Ship Block's Position, y is 128 to stick with the chunk's height limits
  // TODO: Make the world un-relative to the ship block
  val ShipBlockPos: BlockPos = new BlockPos(0, 128, 0)
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

  // TODO: Change this to be the biome directly under the ship
  val BiomeID = OriginWorld.getBiomeGenForCoords(OriginPos()).biomeID

  // The Ship Block
  def ShipBlock = getBlockState(ShipWorld.ShipBlockPos)

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
      BlocksOnShip.add(uPos)
      applyBlockChange(uPos.RelativePos, bs, 0)
    })
    everythingElse.foreach(uPos => {
      val bs = OriginWorld.getBlockState(uPos.WorldPos)
      BlocksOnShip.add(uPos)
      applyBlockChange(uPos.RelativePos, bs, 0)
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

  override def setBlockState(pos: BlockPos, newState: IBlockState, flags: Int): Boolean = pos != ShipWorld.ShipBlockPos && isValid(pos)


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

  def applyBlockChange(pos: BlockPos, newState: IBlockState, flags: Int): Boolean

  def onShipMove(): Unit

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
    val rotatedStart = VectorUtils.rotatePointFromShip(relativeStart, Ship)
    val rotatedEnd = VectorUtils.rotatePointFromShip(relativeEnd, Ship)


    // The result of the ray-trace on the ship world
    return super.rayTraceBlocks(rotatedStart, rotatedEnd)
  }

  def isShipValid = BlocksOnShip.nonEmpty && chunkProvider.asInstanceOf[ChunkProviderShip].ChunkMap.nonEmpty

  protected def isValid(pos: BlockPos) = pos.getX >= -30000000 && pos.getZ >= -30000000 && pos.getX < 30000000 && pos.getZ < 30000000 && pos.getY >= 0 && pos.getY < 256

  override def playSoundEffect(x: Double, y: Double, z: Double, soundName: String, volume: Float, pitch: Float) = {
    val newVec = UnifiedVec.convertToWorld(new Vec3(x, y, z), Ship.getPositionVector)
    OriginWorld.playSoundEffect(newVec.xCoord, newVec.yCoord, newVec.zCoord, soundName, volume, pitch)
  }

  // Ripped from doVoidFogParticles in World class
  // Todo: Make a less laggy way to do this
  @SideOnly(Side.CLIENT)
  def doRandomDisplayTick(posX: Int, posY: Int, posZ: Int) = {
    val i: Int = 16
    val random: Random = new Random
    val itemstack: ItemStack = Minecraft.getMinecraft.thePlayer.getHeldItem
    val flag: Boolean = Minecraft.getMinecraft.playerController.getCurrentGameType == WorldSettings.GameType.CREATIVE && itemstack != null && Block.getBlockFromItem(itemstack.getItem) == Blocks.barrier
    val blockpos$mutableblockpos: BlockPos.MutableBlockPos = new BlockPos.MutableBlockPos

    var j: Int = 0
    while (j < 1000) {
      val k: Int = posX + this.rand.nextInt(i) - this.rand.nextInt(i)
      val l: Int = posY + this.rand.nextInt(i) - this.rand.nextInt(i)
      val i1: Int = posZ + this.rand.nextInt(i) - this.rand.nextInt(i)
      blockpos$mutableblockpos.set(k, l, i1)
      val iblockstate: IBlockState = this.getBlockState(blockpos$mutableblockpos)
      iblockstate.getBlock.randomDisplayTick(this, blockpos$mutableblockpos, iblockstate, random)
      if (flag && iblockstate.getBlock == Blocks.barrier)
        this.spawnParticle(EnumParticleTypes.BARRIER, (k.toFloat + 0.5F).toDouble, (l.toFloat + 0.5F).toDouble, (i1.toFloat + 0.5F).toDouble, 0.0D, 0.0D, 0.0D, 0)

      j += 1
    }
  }


  override def isSideSolid(pos: BlockPos, side: EnumFacing, default: Boolean) = getBlockState(pos).getBlock.isSideSolid(this, pos, side)

  override def getBiomeGenForCoords(pos: BlockPos) = OriginWorld.getBiomeGenForCoords(Ship.getPosition.add(pos))

  override def getBiomeGenForCoordsBody(pos: BlockPos) = OriginWorld.getBiomeGenForCoords(Ship.getPosition.add(pos))


}
