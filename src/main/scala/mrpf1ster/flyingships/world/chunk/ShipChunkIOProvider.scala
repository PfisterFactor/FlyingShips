package mrpf1ster.flyingships.world.chunk

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.chunk.storage.AnvilChunkLoader
import net.minecraftforge.common.util.AsynchronousExecutor

/**
  * Created by EJ on 7/20/2016.
  */
class ShipChunkIOProvider extends AsynchronousExecutor.CallBackProvider[ShipQueuedChunk, Chunk, Runnable, RuntimeException] {
  private val threadNumber: AtomicInteger = new AtomicInteger(1)

  override def callStage1(queuedChunk: ShipQueuedChunk): Chunk = {
    val loader: AnvilChunkLoader = queuedChunk.loader
    var data: Array[AnyRef] = null
    try {
      data = loader.loadChunk__Async(queuedChunk.world, queuedChunk.x, queuedChunk.z)
    }
    catch {
      case e: IOException => e.printStackTrace()
    }

    if (data != null) {
      queuedChunk.compound = data(1).asInstanceOf[NBTTagCompound]
      return data(0).asInstanceOf[Chunk]
    }

    null
  }

  override def callStage2(queuedChunk: ShipQueuedChunk, chunk: Chunk): Unit = {
    if (chunk == null)
      return


    queuedChunk.loader.loadEntities(queuedChunk.world, queuedChunk.compound.getCompoundTag("Level"), chunk)
    chunk.setLastSaveTime(queuedChunk.provider.ShipWorld.getTotalWorldTime)
    queuedChunk.provider.ChunkMap(new ChunkCoordIntPair(queuedChunk.x, queuedChunk.z)) =  chunk
    queuedChunk.provider.LoadedChunks.append(chunk)
    chunk.onChunkLoad()
  }

  override def callStage3(queuedChunk: ShipQueuedChunk, chunk: Chunk, callback: Runnable): Unit = {
    callback.run()
  }



  override def newThread(runnable: Runnable): Thread = {
    val thread: Thread = new Thread(runnable, "Chunk I/O Executor Thread-" + threadNumber.getAndIncrement)
    thread.setDaemon(true)
    thread
  }
}
