package mrpf1ster.flyingships.world

import java.util.{Random, UUID}

import com.google.common.collect.Sets
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.render.ShipRenderGlobal
import mrpf1ster.flyingships.util.UnifiedPos
import mrpf1ster.flyingships.world.chunk.ClientChunkProviderShip
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.{BlockPos, EnumParticleTypes}
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.{ChunkCoordIntPair, World, WorldSettings}

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

  override def onChunkLoad(x: Int, z: Int) = chunkProvider.asInstanceOf[ClientChunkProviderShip].onWorldChunkLoad(x, z)

  override def onChunkUnload(x: Int, z: Int) = chunkProvider.asInstanceOf[ClientChunkProviderShip].onWorldChunkUnload(x, z)


  def doRandomDisplayTick() = {
    val relPlayerPos = UnifiedPos.convertToRelative(Minecraft.getMinecraft.thePlayer.getPosition, Ship.getPosition)
    // Ripped from doVoidFogParticles in World class
    // Todo: Make a less laggy way to do this
    def doRandomDisplayTickAtPos(posX: Int, posY: Int, posZ: Int) = {
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
