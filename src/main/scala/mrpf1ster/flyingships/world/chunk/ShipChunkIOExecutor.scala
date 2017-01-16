package mrpf1ster.flyingships.world.chunk

import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.chunk.storage.AnvilChunkLoader
import net.minecraftforge.common.util.AsynchronousExecutor

/**
  * Created by EJ on 7/20/2016.
  */

// Full copy of ChunkIOExecutor, with some changes to allow non ChunkProviderServers
class ShipChunkIOExecutor {
  val BASE_THREADS: Int = 1
  val PLAYERS_PER_THREAD: Int = 50
  val instance: AsynchronousExecutor[ShipQueuedChunk, Chunk, Runnable, RuntimeException] = new AsynchronousExecutor[ShipQueuedChunk, Chunk, Runnable, RuntimeException](new ShipChunkIOProvider, BASE_THREADS)

  def syncChunkLoad(world: World, loader: AnvilChunkLoader, provider: ChunkProviderShip, x: Int, z: Int): Chunk = {
    instance.getSkipQueue(new ShipQueuedChunk(x, z, loader, world, provider))
  }

  def queueChunkLoad(world: World, loader: AnvilChunkLoader, provider: ChunkProviderShip, x: Int, z: Int, runnable: Runnable) {
    instance.add(new ShipQueuedChunk(x, z, loader, world, provider), runnable)
  }

  def dropQueuedChunkLoad(world: World, x: Int, z: Int, runnable: Runnable) {
    instance.drop(new ShipQueuedChunk(x, z, null, world, null), runnable)
  }

  def adjustPoolSize(players: Int) {
    val size: Int = Math.max(BASE_THREADS, Math.ceil(players / PLAYERS_PER_THREAD).toInt)
    instance.setActiveThreads(size)
  }

  def tick() {
    instance.finishActive()
  }
}
