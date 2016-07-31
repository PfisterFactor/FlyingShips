package mrpf1ster.flyingships.world.chunk

import mrpf1ster.flyingships.util.BlockUtils
import mrpf1ster.flyingships.world.{ShipWorld, ShipWorldClient}
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.chunk.Chunk

/**
  * Created by ej on 7/25/16.
  */
class ClientChunkProviderShip(shipWorld: ShipWorld) extends ChunkProviderShip(shipWorld, null) {
  override def saveChunkData(chunk: Chunk) = {}

  override def saveChunkExtraData(chunk: Chunk) = {}

  override def canSave = false

  def onWorldChunkUnload(chunkX: Int, chunkZ: Int) = {
    val chunkCoords = BlockUtils.getRelativeChunkFromWorld(chunkX, chunkZ, shipWorld.OriginPos())

    if (shipWorld.ChunksOnShip.contains(chunkCoords)) {
      shipWorld.asInstanceOf[ShipWorldClient].ChunksToRender.remove(chunkCoords)
      shipWorld.asInstanceOf[ShipWorldClient].doRenderUpdate = true
    }

  }

  def onWorldChunkLoad(chunkX: Int, chunkZ: Int) = {
    val chunkCoords = BlockUtils.getRelativeChunkFromWorld(chunkX, chunkZ, shipWorld.OriginPos())
    if (shipWorld.ChunksOnShip.contains(chunkCoords)) {
      shipWorld.asInstanceOf[ShipWorldClient].ChunksToRender.add(chunkCoords)
      shipWorld.asInstanceOf[ShipWorldClient].doRenderUpdate = true
    }

  }
  override def loadChunk(x: Int, z: Int, runnable: Runnable): Chunk = {
    DroppedChunks.remove(new ChunkCoordIntPair(x, z))
    val newChunk = new Chunk(shipWorld, x, z)
    ChunkMap(new ChunkCoordIntPair(x, z)) = newChunk
    LoadedChunks.append(newChunk)
    newChunk.onChunkLoad()
    if (runnable != null)
      runnable.run()
    newChunk
  }
}
