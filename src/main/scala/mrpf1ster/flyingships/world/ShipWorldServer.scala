package mrpf1ster.flyingships.world

import java.util.UUID

import com.google.common.collect.{Lists, Sets}
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.network.BlockActionMessage
import mrpf1ster.flyingships.util.UnifiedPos
import mrpf1ster.flyingships.world.chunk.ChunkProviderShip
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.block.{Block, BlockEventData}
import net.minecraft.crash.{CrashReport, CrashReportCategory}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{BlockPos, ReportedException}
import net.minecraft.world.chunk.storage.AnvilChunkLoader
import net.minecraft.world.chunk.{Chunk, IChunkProvider}
import net.minecraft.world.gen.structure.StructureBoundingBox
import net.minecraft.world.{ChunkCoordIntPair, NextTickListEntry, World}
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.{Set => mSet}

/**
  * Created by ej on 7/25/16.
  */

class ShipWorldServer(originWorld: World, ship: EntityShip, uUID: UUID) extends ShipWorld(originWorld, ship, uUID) {

  private val ServerBlockEventList = mutable.Set[BlockEventData]()

  var PlayerManager: PlayerManagerShip = null
  val WorldManager = new ShipWorldManager(this)
  addWorldAccess(WorldManager)

  // Stuff for compatibility with WorldServer methods
  private val pendingTickListEntriesHashSet: java.util.Set[NextTickListEntry] = Sets.newHashSet[NextTickListEntry]
  private val pendingTickListEntriesTreeSet: java.util.TreeSet[NextTickListEntry] = new java.util.TreeSet[NextTickListEntry]
  private val pendingTickListEntriesThisTick: java.util.List[NextTickListEntry] = Lists.newArrayList[NextTickListEntry]

  // Defer the creation until blocks are loaded onto the ship
  def createPlayerManager() = {
    PlayerManager = new PlayerManagerShip(this)
  }
  override def createChunkProvider(): IChunkProvider = {
    val anvilLoader = new AnvilChunkLoader(saveHandler.getWorldDirectory)
    new ChunkProviderShip(this, anvilLoader)
  }

  def getChunkProviderServer: ChunkProviderShip = chunkProvider.asInstanceOf[ChunkProviderShip]

  override def tick(): Unit = {
    tickUpdates(false)
    updateBlocks()
    //worldTeleporter.removeStalePortalLocations(this.getTotalWorldTime)
    //customTeleporters.foreach(tele => tele.removeStalePortalLocations(getTotalWorldTime))
    this.chunkProvider.unloadQueuedChunks()
    this.worldInfo.setWorldTotalTime(OriginWorld.getTotalWorldTime)
    PlayerManager.updatePlayerInstances()

    sendQueuedBlockEvents()
  }

  override def onChunkLoad(x: Int, z: Int) = PlayerManager.onWorldChunkLoad(x, z)

  override def onChunkUnload(x: Int, z: Int) = PlayerManager.onWorldChunkUnload(x, z)

  override def addBlockEvent(pos: BlockPos, block: Block, eventID: Int, eventParam: Int) = ServerBlockEventList.add(new BlockEventData(pos, block, eventID, eventParam))

  private def sendQueuedBlockEvents() = {
    ServerBlockEventList.foreach(event => {
      if (fireBlockEvent(event)) {
        val message = new BlockActionMessage(event.getPosition, event.getBlock, event.getEventID, event.getEventParameter, Ship.ShipID)
        val blockPos = UnifiedPos.convertToWorld(event.getPosition, OriginPos())
        val targetPoint = new TargetPoint(OriginWorld.provider.getDimensionId, blockPos.getX, blockPos.getY, blockPos.getZ, 64)
        FlyingShips.flyingShipPacketHandler.INSTANCE.sendToAllAround(message, targetPoint)
      }

    })
    ServerBlockEventList.clear()

  }

  private def fireBlockEvent(event: BlockEventData): Boolean = {
    val iblockstate: IBlockState = this.getBlockState(event.getPosition)

    if (iblockstate.getBlock == event.getBlock)
      iblockstate.getBlock.onBlockEventReceived(this, event.getPosition, iblockstate, event.getEventID, event.getEventParameter)
    else
      false
  }

  def saveAllChunks(extraData: Boolean): Unit = {
    if (!chunkProvider.canSave) return
    chunkProvider.saveChunks(extraData, null)
    chunkProvider.asInstanceOf[ChunkProviderShip].LoadedChunks.foreach(chunk => {
      if (!PlayerManager.hasPlayerInstance(chunk.xPosition, chunk.zPosition))
        chunkProvider.asInstanceOf[ChunkProviderShip].dropChunk(chunk.xPosition, chunk.zPosition)
    })
  }

  def getTileEntitiesIn(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int): java.util.List[TileEntity] = {
    val list: java.util.List[TileEntity] = Lists.newArrayList[TileEntity]
    var x: Int = minX & ~0x0F
    while (x < maxX) {
      var z: Int = minZ & ~0x0F
      while (z < maxZ) {
        if (this.isChunkLoaded(x >> 4, z >> 4, true)) {
          val chunk: Chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4)
          if (chunk != null && !chunk.isEmpty) {
            for (entity <- chunk.getTileEntityMap.values) {
              if (!entity.isInvalid) {
                val pos: BlockPos = entity.getPos
                if (pos.getX >= minX && pos.getY >= minY && pos.getZ >= minZ && pos.getX < maxX && pos.getY < maxY && pos.getZ < maxZ) {
                  list.add(entity)
                }
              }
            }
          }
        }
        z += 16
      }
      x += 16
    }
    list
  }

  override def isBlockTickPending(blockPos: BlockPos, block: Block): Boolean = pendingTickListEntriesThisTick.contains(new NextTickListEntry(blockPos, block))

  override def scheduleUpdate(pos: BlockPos, blockIn: Block, delay: Int): Unit = this.updateBlockTick(pos, blockIn, delay, 0)

  override def updateBlockTick(pos: BlockPos, blockIn: Block, delay: Int, priority: Int): Unit = {
    val nextticklistentry: NextTickListEntry = new NextTickListEntry(pos, blockIn)
    var newDelay = delay
    var i: Int = 0

    if (this.scheduledUpdatesAreImmediate && blockIn.getMaterial != Material.air) {
      if (blockIn.requiresUpdates) {
        i = 8
        val isForced: Boolean = getPersistentChunks.containsKey(new ChunkCoordIntPair(nextticklistentry.position.getX >> 4, nextticklistentry.position.getZ >> 4))
        i = if (isForced) 0
        else 8
        if (this.isAreaLoaded(nextticklistentry.position.add(-i, -i, -i), nextticklistentry.position.add(i, i, i))) {
          val iblockstate: IBlockState = this.getBlockState(nextticklistentry.position)
          if (iblockstate.getBlock.getMaterial != Material.air && iblockstate.getBlock == nextticklistentry.getBlock) {
            iblockstate.getBlock.updateTick(this, nextticklistentry.position, iblockstate, this.rand)
          }
        }
        return
      }
      newDelay = 1
    }

    if (this.isAreaLoaded(pos.add(-i, -i, -i), pos.add(i, i, i))) {
      if (blockIn.getMaterial != Material.air) {
        nextticklistentry.setScheduledTime(newDelay.toLong + this.worldInfo.getWorldTotalTime)
        nextticklistentry.setPriority(priority)
      }
      if (!this.pendingTickListEntriesHashSet.contains(nextticklistentry)) {
        this.pendingTickListEntriesHashSet.add(nextticklistentry)
        this.pendingTickListEntriesTreeSet.add(nextticklistentry)
      }
    }
  }

  override def scheduleBlockUpdate(pos: BlockPos, blockIn: Block, delay: Int, priority: Int): Unit = {
    val nextticklistentry: NextTickListEntry = new NextTickListEntry(pos, blockIn)
    nextticklistentry.setPriority(priority)

    if (blockIn.getMaterial != Material.air)
      nextticklistentry.setScheduledTime(delay.toLong + this.worldInfo.getWorldTotalTime)

    if (!this.pendingTickListEntriesHashSet.contains(nextticklistentry)) {
      this.pendingTickListEntriesHashSet.add(nextticklistentry)
      this.pendingTickListEntriesTreeSet.add(nextticklistentry)
    }
  }

  override def getPendingBlockUpdates(chunkIn: Chunk, par2: Boolean): java.util.List[NextTickListEntry] = {
    val chunkcoordintpair: ChunkCoordIntPair = chunkIn.getChunkCoordIntPair
    val i: Int = (chunkcoordintpair.chunkXPos << 4) - 2
    val j: Int = i + 16 + 2
    val k: Int = (chunkcoordintpair.chunkZPos << 4) - 2
    val l: Int = k + 16 + 2
    this.func_175712_a(new StructureBoundingBox(i, 0, k, j, 256, l), par2)
  }

  override def func_175712_a(structureBB: StructureBoundingBox, par2: Boolean): java.util.List[NextTickListEntry] = {
    var list: java.util.List[NextTickListEntry] = null
    var i: Int = 0
    while (i < 2) {
      var iterator: java.util.Iterator[NextTickListEntry] = null
      if (i == 0)
        iterator = this.pendingTickListEntriesTreeSet.iterator
      else
        iterator = this.pendingTickListEntriesThisTick.iterator

      while (iterator.hasNext) {
        val nextticklistentry: NextTickListEntry = iterator.next
        val blockpos: BlockPos = nextticklistentry.position
        if (blockpos.getX >= structureBB.minX && blockpos.getX < structureBB.maxX && blockpos.getZ >= structureBB.minZ && blockpos.getZ < structureBB.maxZ) {
          if (par2) {
            this.pendingTickListEntriesHashSet.remove(nextticklistentry)
            iterator.remove()
          }
          if (list == null) {
            list = Lists.newArrayList[NextTickListEntry]
          }
          list.add(nextticklistentry)
        }
      }
      i += 1
    }
    list
  }

  override def tickUpdates(par1: Boolean): Boolean = {

    var i: Int = this.pendingTickListEntriesTreeSet.size
    if (i != this.pendingTickListEntriesHashSet.size) throw new IllegalStateException("TickNextTick list out of synch")
    if (i > 1000) i = 1000

    //this.theProfiler.startSection("cleaning")
    var j: Int = 0
    def cleanUp(): Unit = while (j < i) {
      val nextticklistentry: NextTickListEntry = this.pendingTickListEntriesTreeSet.first
      if (!par1 && nextticklistentry.scheduledTime > this.worldInfo.getWorldTotalTime) {
        return
      }
      this.pendingTickListEntriesTreeSet.remove(nextticklistentry)
      this.pendingTickListEntriesHashSet.remove(nextticklistentry)
      this.pendingTickListEntriesThisTick.add(nextticklistentry)
      j += 1
    }
    cleanUp()

    //this.theProfiler.endSection
    //this.theProfiler.startSection("ticking")
    val iterator: java.util.Iterator[NextTickListEntry] = this.pendingTickListEntriesThisTick.iterator
    while (iterator.hasNext) {

      val nextticklistentry1: NextTickListEntry = iterator.next
      iterator.remove()
      val k: Int = 0
      if (this.isAreaLoaded(nextticklistentry1.position.add(-k, -k, -k), nextticklistentry1.position.add(k, k, k))) {
        val iblockstate: IBlockState = this.getBlockState(nextticklistentry1.position)
        if (iblockstate.getBlock.getMaterial != Material.air && Block.isEqualTo(iblockstate.getBlock, nextticklistentry1.getBlock)) {
          try {
            iblockstate.getBlock.updateTick(this, nextticklistentry1.position, iblockstate, this.rand)
          }
          catch {
            case throwable: Throwable =>
              val crashreport: CrashReport = CrashReport.makeCrashReport(throwable, "Exception while ticking a block")
              val crashreportcategory: CrashReportCategory = crashreport.makeCategory("Block being ticked")
              CrashReportCategory.addBlockInfo(crashreportcategory, nextticklistentry1.position, iblockstate)
              throw new ReportedException(crashreport)
          }
        }
      }
      else {
        this.scheduleUpdate(nextticklistentry1.position, nextticklistentry1.getBlock, 0)
      }

    }
    //this.theProfiler.endSection
    this.pendingTickListEntriesThisTick.clear()
    !this.pendingTickListEntriesTreeSet.isEmpty

  }

  override def updateBlocks(): Unit = {
    super.updateBlocks()

    var i: Int = 0
    var j: Int = 0

    for (chunkcoordintpair <- this.activeChunkSet) {
      val k: Int = chunkcoordintpair.chunkXPos * 16
      val l: Int = chunkcoordintpair.chunkZPos * 16
      //his.theProfiler.startSection("getChunk")
      val chunk: Chunk = this.getChunkFromChunkCoords(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos)
      this.playMoodSoundAndCheckLight(k, l, chunk)
      //this.theProfiler.endStartSection("tickChunk")
      chunk.func_150804_b(false)
      //this.theProfiler.endStartSection("thunder")
      //this.theProfiler.endStartSection("iceandsnow")
      /*
      Todo: Add snow on Shipworlds
      if (this.provider.canDoRainSnowIce(chunk) && this.rand.nextInt(16) == 0) {
        this.updateLCG = this.updateLCG * 3 + 1013904223
        val k2: Int = this.updateLCG >> 2
        val blockpos2: Blockpos = this.getPrecipitationHeight(new Blockpos(k + (k2 & 15), 0, l + (k2 >> 8 & 15)))
        val blockpos1: Blockpos = blockpos2.down
        if (this.canBlockFreezeNoWater(blockpos1)) {
          this.setBlockState(blockpos1, Blocks.ice.getDefaultState)
        }
        if (this.isRaining && this.canSnowAt(blockpos2, true)) {
          this.setBlockState(blockpos2, Blocks.snow_layer.getDefaultState)
        }
        if (this.isRaining && this.getBiomeGenForCoords(blockpos1).canSpawnLightningBolt) {
          this.getBlockState(blockpos1).getBlock.fillWithRain(this, blockpos1)
        }
      }
      */
      //this.theProfiler.endStartSection("tickBlocks")
      val l2: Int = OriginWorld.getGameRules.getInt("randomTickSpeed")
      if (l2 <= 0) return

      for (extendedblockstorage <- chunk.getBlockStorageArray) {
        if (extendedblockstorage != null && extendedblockstorage.getNeedsRandomTick) {
          var j1: Int = 0
          while (j1 < l2) {
            this.updateLCG = this.updateLCG * 3 + 1013904223
            val k1: Int = this.updateLCG >> 2
            val l1: Int = k1 & 15
            val i2: Int = k1 >> 8 & 15
            val j2: Int = k1 >> 16 & 15
            j += 1
            val iblockstate: IBlockState = extendedblockstorage.get(l1, j2, i2)
            val block: Block = iblockstate.getBlock
            if (block.getTickRandomly) {
              i += 1
              block.randomTick(this, new BlockPos(l1 + k, j2 + extendedblockstorage.getYLocation, i2 + l), iblockstate, this.rand)
            }
            j1 += 1
          }
        }
      }
    }
  }

  private def adjustPosToNearbyEntity(pos: BlockPos): BlockPos = {
    val blockpos: BlockPos = this.getPrecipitationHeight(pos)
    //val axisalignedbb: AxisAlignedBB = new AxisAlignedBB(blockpos, new Blockpos(blockpos.getX, this.getHeight, blockpos.getZ)).expand(3.0D, 3.0D, 3.0D)
    /*
    val list = this.getEntitiesWithinAABB(classOf[EntityLivingBase], axisalignedbb, new Predicate[EntityLivingBase]() {
      def apply(p_apply_1_:

      EntityLivingBase): Boolean = {
        return p_apply_1_ != null && p_apply_1_.isEntityAlive && thisWorldServer.canSeeSky(p_apply_1_.getPosition)
      }
    })
    */
    //if (!list.isEmpty) list.get(this.rand.nextInt(list.size)).asInstanceOf[EntityLivingBase].getPosition
    /* else */
    blockpos
  }
}
