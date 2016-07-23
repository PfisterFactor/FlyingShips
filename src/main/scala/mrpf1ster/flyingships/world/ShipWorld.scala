package mrpf1ster.flyingships.world

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import java.util
import java.util.{Random, UUID}

import com.google.common.base.Predicate
import io.netty.buffer.Unpooled
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.network.{BlockActionMessage, BlocksChangedMessage}
import mrpf1ster.flyingships.util.{ShipLocator, UnifiedPos, UnifiedVec, VectorUtils}
import mrpf1ster.flyingships.world.chunk.ChunkProviderShip
import net.minecraft.block.state.IBlockState
import net.minecraft.block.{Block, BlockEventData}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{Entity, EntityHanging}
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util._
import net.minecraft.world.chunk.storage.AnvilChunkLoader
import net.minecraft.world.chunk.{Chunk, IChunkProvider}
import net.minecraft.world.{IInteractionObject, World, WorldSettings}
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

import scala.collection.JavaConversions._
import scala.collection.mutable
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
    ShipLocator.getShips(world).foreach(ent => ent.ShipWorld.wasAccessed = false)
  }
}

class ShipWorld(originWorld: World, ship: EntityShip) extends DetachedWorld(originWorld, "Ship", UUID.randomUUID()) {


  val OriginWorld = originWorld

  val Ship = ship

  // The coordinates of the ship block in the origin world. Conveniently the EntityShip's position
  def OriginPos() = Ship.getPosition

  def OriginVec() = Ship.getPositionVector

  chunkProvider = createChunkProvider()

  val BlocksOnShip: mSet[UnifiedPos] = mSet()

  // The Ship Block
  def ShipBlock = getBlockState(ShipWorld.ShipBlockPos)


  // TODO: Change this to be the biome directly under the ship
  val BiomeID = OriginWorld.getBiomeGenForCoords(OriginPos()).biomeID

  // All TileEntities on Ship, mapped with a Unified Pos
  var TileEntities: mMap[UnifiedPos, TileEntity] = moveTileEntitiesOntoShip

  def TickableTileEntities: Map[UnifiedPos, TileEntity] = Map(TileEntities.filter(tuple => tuple._2.isInstanceOf[ITickable]).toSeq: _*)

  // All HangingEntities on ship, mapped with a Unified Pos
  // #Not implemented#
  var HangingEntities: mMap[UnifiedPos, EntityHanging] = null

  private val ChangedBlocks: mSet[UnifiedPos] = mSet()
  private var doRenderUpdate = false

  // Only used on Server
  private val ServerBlockEventList = mutable.Set[BlockEventData]()


  def moveBlocksOnShip(blocks: Set[UnifiedPos]) = {
    blocks.foreach(uPos => {
      val blockstate = OriginWorld.getBlockState(uPos.WorldPos)
      if (blockstate != null && blockstate.getBlock != Blocks.air) {
        BlocksOnShip.add(uPos)
        setBlockState(uPos.RelativePos, blockstate, 0)
      }
    })
  }

  private def moveTileEntitiesOntoShip: mMap[UnifiedPos, TileEntity] = {
    if (!isValid)
      return mMap()
    mMap(BlocksOnShip
      .filter(uPos => OriginWorld.getTileEntity(uPos.WorldPos) != null)
      .map(tileEntityUPos => {
        val tileEntity = OriginWorld.getTileEntity(tileEntityUPos.WorldPos)
        var copyTileEntity: TileEntity = null
        try {
          val nbt = new NBTTagCompound
          tileEntity.writeToNBT(nbt)
          val teOnShip = getTileEntity(tileEntityUPos.RelativePos)
          if (teOnShip != null) {
            teOnShip.readFromNBT(nbt)
            teOnShip.validate()
            teOnShip.setWorldObj(this)
          }
          else {
            copyTileEntity = TileEntity.createAndLoadEntity(nbt)
            copyTileEntity.setWorldObj(this)
            copyTileEntity.setPos(tileEntityUPos.RelativePos)
            copyTileEntity.validate()
          }

        }
        catch {
          case ex: Exception => println(s"There was an error moving TileEntity ${tileEntity.getClass.getName} at ${tileEntityUPos.WorldPos}\n$ex") // Error reporting
        }
        tileEntityUPos -> copyTileEntity // Return our copied to ship tile entity for the map function
      }).toSeq: _*)
  }


  /*
  override def getBlockState(pos: BlockPos) = {
    val got = BlockStore.getBlock(pos)
    if (got.isDefined)
      got.get.BlockState // Got get got get got get
    else
      Block.getStateById(0)

  }
  */

  override def createChunkProvider(): IChunkProvider = {
    val anvilLoader = new AnvilChunkLoader(saveHandler.getWorldDirectory)
    new ChunkProviderShip(this, anvilLoader)
  }

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


  override def markBlockForUpdate(pos: BlockPos) = {
    ChangedBlocks.add(UnifiedPos(pos, OriginPos, true))
    pushBlockChangesToClient()
  }


  override def tick(): Unit = {
    if (isRemote) return

    //tickUpdates(false)
    //updateBlocks()
    //worldTeleporter.removeStalePortalLocations(this.getTotalWorldTime)
    //customTeleporters.foreach(tele => tele.removeStalePortalLocations(getTotalWorldTime))
    this.chunkProvider.unloadQueuedChunks()

    sendQueuedBlockEvents()
  }

  // TODO: (Potentially?) fix this for large ships, maybe individual blocks at a time
  def pushBlockChangesToClient(): Unit = {
    if (!isValid) return
    if (Ship == null) return
    if (ChangedBlocks.isEmpty) return

    val message = new BlocksChangedMessage(Ship, ChangedBlocks.map(pos => pos.RelativePos).toArray)
    val targetPoint = new TargetPoint(OriginWorld.provider.getDimensionId, Ship.getPosition.getX, Ship.getPosition.getY, Ship.getPosition.getZ, 64)
    FlyingShips.flyingShipPacketHandler.INSTANCE.sendToAllAround(message, targetPoint)

    ChangedBlocks.clear()

  }

  override def spawnEntityInWorld(entity: Entity): Boolean = {
    val worldPos = UnifiedVec.convertToWorld(entity.getPositionVector, Ship.getPositionVector)

    entity.setPosition(worldPos.xCoord, worldPos.yCoord, worldPos.zCoord)
    entity.setWorld(OriginWorld)

    OriginWorld.spawnEntityInWorld(entity)
  }

  override def setBlockState(pos: BlockPos, newState: IBlockState, flags: Int): Boolean = {
    if (isRemote) return true
    if (pos == ShipWorld.ShipBlockPos) return false
    return applyBlockChange(pos, newState, flags)

  }

  var wasAccessed = false

  override def getTileEntity(pos: BlockPos) = {
    val te = super.getTileEntity(pos)
    if (ShipWorld.doAccessing) {
      wasAccessed = te.isInstanceOf[IInteractionObject]
    }
    else
      wasAccessed = false
    te
  }
  def applyBlockChange(pos: BlockPos, newState: IBlockState, flags: Int): Boolean = {
    val uPos = UnifiedPos(pos, OriginPos, true)
    val contains = BlocksOnShip.contains(uPos)
    BlocksOnShip.add(uPos)
    if (!contains && newState.getBlock != Blocks.air) {
      Ship.generateBoundingBox()
    }
    else if (newState.getBlock == Blocks.air) {
      BlocksOnShip.remove(uPos)
      Ship.generateBoundingBox()
    }




    val chunk: Chunk = this.getChunkFromBlockCoords(pos)
    val block: Block = newState.getBlock
    val oldBlock: Block = getBlockState(pos).getBlock
    val oldLight: Int = oldBlock.getLightValue(this, pos)
    val oldOpacity: Int = oldBlock.getLightOpacity(this, pos)
    val iblockstate: IBlockState = chunk.setBlockState(pos, newState)
    if (iblockstate == null)
      return false

    val block1: Block = iblockstate.getBlock

    if (block.getLightOpacity(this, pos) != oldOpacity || block.getLightValue(this, pos) != oldLight) {
      this.checkLight(pos)
    }

    this.markAndNotifyBlock(pos, chunk, iblockstate, newState, flags)
    if (!isRemote && (flags & 2) != 0) {
      ChangedBlocks.add(new UnifiedPos(pos, Ship.getPosition, true))
      pushBlockChangesToClient()
    }
    doRenderUpdate = true
    return true
  }

  def onShipMove() = {
    doRenderUpdate = true
  }

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


  override def addBlockEvent(pos: BlockPos, block: Block, eventID: Int, eventParam: Int): Unit = {
    if (isRemote) {
      block.onBlockEventReceived(this, pos, getBlockState(pos), eventID, eventParam)
      return
    }

    ServerBlockEventList.add(new BlockEventData(pos, block, eventID, eventParam))
  }

  // Only used on Server
  private def sendQueuedBlockEvents() = {
    ServerBlockEventList.foreach(event => {
      if (fireBlockEvent(event)) {
        val message = new BlockActionMessage(event.getPosition, event.getBlock, event.getEventID, event.getEventParameter, Ship.getEntityId)
        val blockPos = UnifiedPos.convertToWorld(event.getPosition, OriginPos())
        val targetPoint = new TargetPoint(OriginWorld.provider.getDimensionId, blockPos.getX, blockPos.getY, blockPos.getZ, 64)
        FlyingShips.flyingShipPacketHandler.INSTANCE.sendToAllAround(message, targetPoint)
      }

    })
    ServerBlockEventList.clear()

  }

  // Only used on Server
  private def fireBlockEvent(event: BlockEventData): Boolean = {
    val iblockstate: IBlockState = this.getBlockState(event.getPosition)

    if (iblockstate.getBlock == event.getBlock)
      iblockstate.getBlock.onBlockEventReceived(this, event.getPosition, iblockstate, event.getEventID, event.getEventParameter)
    else
      false
  }

  // Ray traces blocks on ship, arguments are non-relative
  // It rotates the look and ray vector against the ship's current rotation so we can use Minecraft's built in world block ray-trace
  def rotatedRayTrace(start: Vec3, end: Vec3): Option[MovingObjectPosition] = {

    val relativeStart = UnifiedVec.convertToRelative(start, Ship.getPositionVector)
    val relativeEnd = UnifiedVec.convertToRelative(end, Ship.getPositionVector)

    // The eye position and ray, rotated by the ship block's center, to the opposite of the ships current rotation
    val rotatedStart = VectorUtils.rotatePointFromShip(relativeStart, Ship)
    val rotatedEnd = VectorUtils.rotatePointFromShip(relativeEnd, Ship)

    // The result of the ray-trace on the ship world
    val rayTrace = Ship.ShipWorld.rayTraceBlocks(rotatedStart, rotatedEnd)

    if (rayTrace != null && rayTrace.typeOfHit == MovingObjectType.BLOCK)
      Some(rayTrace)
    else
      None

  }

  def getWorldData: (Array[Byte], Array[Byte]) = {
    // Block Data
    val blockByte = new ByteArrayOutputStream()
    val out = new DataOutputStream(blockByte)


    // Length of map
    out.writeInt(BlocksOnShip.size)

    // BlockPos
    BlocksOnShip.foreach(pos => out.writeLong(pos.RelativePos.toLong))

    // BlockStates
    //noinspection ScalaDeprecation
    BlocksOnShip.foreach(uPos => out.writeInt(Block.BLOCK_STATE_IDS.get(getBlockState(uPos.RelativePos))))

    out.close()

    val blockData = blockByte.toByteArray


    // Tile Entities
    val buffer = new PacketBuffer(Unpooled.buffer())

    buffer.writeInt(TileEntities.size)
    TileEntities.foreach(pair => {
      def uPos = pair._1
      def te = pair._2
      val nbt = new NBTTagCompound()
      te.writeToNBT(nbt)
      buffer.writeNBTTagCompoundToBuffer(nbt)
    })

    val tileEntData = buffer.array()

    (blockData, tileEntData)
  }

  def setWorldData(blockData: Array[Byte], tileEntData: Array[Byte]) = {
    // Block Data
    val byteStream = new ByteArrayInputStream(blockData)
    val in = new DataInputStream(byteStream)

    // Length of map
    val mapLength = in.readInt()

    // BlockPos
    val blockpositions = new Array[UnifiedPos](mapLength)
    for (i: Int <- 0 until mapLength)
      blockpositions(i) = UnifiedPos(BlockPos.fromLong(in.readLong()), OriginPos, IsRelative = true)

    // Block Storage
    val blockstorages = new Array[BlockStorage](mapLength)
    //noinspection ScalaDeprecation
    for (i <- 0 until mapLength)
      blockstorages(i) = new BlockStorage(Block.BLOCK_STATE_IDS.getByValue(in.readInt()))

    in.close()
    (0 until mapLength).foreach(i => applyBlockChange(blockpositions(i).RelativePos, blockstorages(i).BlockState, 3))

    val buffer = new PacketBuffer(Unpooled.copiedBuffer(tileEntData))

    val teSize = buffer.readInt()
    val tileentities = new Array[TileEntity](teSize)
    for (i <- 0 until teSize)
      tileentities(i) = TileEntity.createAndLoadEntity(buffer.readNBTTagCompoundFromBuffer())

    // Map tile entity positions to UnifiedPositions and then zip it with the tile entities array
    TileEntities = mMap(tileentities.map(te => UnifiedPos(te.getPos, OriginPos, IsRelative = true)).zip(tileentities).toSeq: _*)
  }

  def isValid = BlocksOnShip.nonEmpty

  def needsRenderUpdate() = doRenderUpdate // debug

  def onRenderUpdate() = {
    doRenderUpdate = false
  }

  override def playSoundEffect(x: Double, y: Double, z: Double, soundName: String, volume: Float, pitch: Float) = {
    val newVec = UnifiedVec.convertToWorld(new Vec3(x, y, z), ship.getPositionVector)

    OriginWorld.playSoundEffect(newVec.xCoord, newVec.yCoord, newVec.zCoord, soundName, volume, pitch)
  }

  override def playAuxSFXAtEntity(player: EntityPlayer, sfxType: Int, pos: BlockPos, par4: Int) = {
    val newPos = UnifiedPos.convertToWorld(pos, ship.getPosition)
    OriginWorld.playAuxSFXAtEntity(player, sfxType, pos, par4)
  }

  override def spawnParticle(particleType: EnumParticleTypes, x: Double, y: Double, z: Double, xOffset: Double, yOffset: Double, zOffset: Double, par8: Int*) = {
    val rotatedPos = UnifiedVec.convertToWorld(VectorUtils.rotatePointToShip(new Vec3(x, y, z), Ship), Ship.getPositionVector)
    OriginWorld.spawnParticle(particleType, rotatedPos.xCoord, rotatedPos.yCoord, rotatedPos.zCoord, xOffset + Ship.motionX, yOffset + Ship.motionY, zOffset + Ship.motionZ, 0)
  }


  // Ripped from doVoidFogParticles in World class
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
