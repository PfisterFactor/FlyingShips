package mrpf1ster.flyingships.world.chunk

import mrpf1ster.flyingships.world.ShipWorld
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.chunk.Chunk

/**
  * Created by ej on 7/25/16.
  */
class ClientChunkProviderShip(shipWorld: ShipWorld) extends ChunkProviderShip(shipWorld, null) {
  override def saveChunkData(chunk: Chunk) = {}

  override def saveChunkExtraData(chunk: Chunk) = {}

  override def canSave = false

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
