package mrpf1ster.flyingships.world

import java.util.UUID

import com.google.common.collect.Sets
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.render.ShipRenderGlobal
import mrpf1ster.flyingships.util.UnifiedPos
import mrpf1ster.flyingships.world.chunk.ClientChunkProviderShip
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.{ChunkCoordIntPair, World}

import scala.collection.JavaConversions._

/**
  * Created by ej on 7/25/16.
  */
class ShipWorldClient(originWorld: World, ship: EntityShip) extends ShipWorld(originWorld, ship, new UUID(0, 0)) {
  var doRenderUpdate = false
  val ShipRenderGlobal = new ShipRenderGlobal(this)
  private val previousActiveChunkSet: java.util.Set[ChunkCoordIntPair] = Sets.newHashSet[ChunkCoordIntPair]

  addWorldAccess(ShipRenderGlobal)


  override def createChunkProvider() = {
    new ClientChunkProviderShip(this)
  }
  override def applyBlockChange(pos: BlockPos, newState: IBlockState, flags: Int): Boolean = {
    val uPos = UnifiedPos(pos, OriginPos, IsRelative = true)
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

    if (iblockstate == null) return false
    if (block.getLightOpacity(this, pos) != oldOpacity || block.getLightValue(this, pos) != oldLight)
      this.checkLight(pos)


    this.markAndNotifyBlock(pos, chunk, iblockstate, newState, flags)

    doRenderUpdate = true
    true
  }

  override def tick() = {
    chunkProvider.unloadQueuedChunks()
    updateBlocks()
  }

  override def setBlockState(pos: BlockPos, newState: IBlockState, flags: Int): Boolean = super.setBlockState(pos, newState, flags)

  override def onShipMove() = {
    doRenderUpdate = true
  }

  override def addBlockEvent(pos: BlockPos, block: Block, eventID: Int, eventParam: Int) = block.onBlockEventReceived(this, pos, getBlockState(pos), eventID, eventParam)

  override def updateBlocks(): Unit = {
    super.updateBlocks
    this.previousActiveChunkSet.retainAll(this.activeChunkSet)

    if (this.previousActiveChunkSet.size == this.activeChunkSet.size) {
      this.previousActiveChunkSet.clear
    }

    var i: Int = 0
    for (chunkcoordintpair <- this.activeChunkSet) {
      if (!this.previousActiveChunkSet.contains(chunkcoordintpair)) {
        val j: Int = chunkcoordintpair.chunkXPos * 16
        val k: Int = chunkcoordintpair.chunkZPos * 16
        //this.theProfiler.startSection("getChunk")
        val chunk: Chunk = this.getChunkFromChunkCoords(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos)
        this.playMoodSoundAndCheckLight(j, k, chunk)
        //this.theProfiler.endSection
        this.previousActiveChunkSet.add(chunkcoordintpair)
        i += 1
        if (i >= 10) {
          return
        }
      }
    }
  }

}
