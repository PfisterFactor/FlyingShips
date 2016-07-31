package mrpf1ster.flyingships.world

import java.util.UUID

import com.google.common.collect.Sets
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.render.ShipRenderGlobal
import mrpf1ster.flyingships.world.chunk.ClientChunkProviderShip
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.{ChunkCoordIntPair, World}

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
  * Created by ej on 7/25/16.
  */
class ShipWorldClient(originWorld: World, ship: EntityShip) extends ShipWorld(originWorld, ship, new UUID(0, 0)) {
  var doRenderUpdate = false
  val ShipRenderGlobal = new ShipRenderGlobal(this)
  val ChunksToRender: mutable.HashSet[ChunkCoordIntPair] = mutable.HashSet()
  private val previousActiveChunkSet: java.util.Set[ChunkCoordIntPair] = Sets.newHashSet[ChunkCoordIntPair]

  addWorldAccess(ShipRenderGlobal)


  override def createChunkProvider() = {
    new ClientChunkProviderShip(this)
  }

  override def isBlockLoaded(pos: BlockPos, allowEmpty: Boolean) = {
    isValid(pos) && ChunksToRender.contains(new ChunkCoordIntPair(pos.getX >> 4, pos.getZ >> 4))
  }
  override def tick() = {
    chunkProvider.unloadQueuedChunks()
    updateBlocks()
  }

  override def onShipMove() = {
    doRenderUpdate = true
  }

  override def addBlockEvent(pos: BlockPos, block: Block, eventID: Int, eventParam: Int) = block.onBlockEventReceived(this, pos, getBlockState(pos), eventID, eventParam)

  override def setBlockState(pos: BlockPos, newState: IBlockState, flags: Int) = {
    val result = super.setBlockState(pos, newState, flags)
    if (result && newState != Blocks.air.getDefaultState) {
      ChunksToRender.add(new ChunkCoordIntPair(pos.getX >> 4, pos.getZ >> 4))
    }
    doRenderUpdate = true

    result
  }
  override def updateBlocks(): Unit = {
    super.updateBlocks()
    this.previousActiveChunkSet.retainAll(this.activeChunkSet)

    if (this.previousActiveChunkSet.size == this.activeChunkSet.size) {
      this.previousActiveChunkSet.clear()
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
