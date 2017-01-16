package mrpf1ster.flyingships.world.chunk

import java.util

import mrpf1ster.flyingships.world.ShipWorld
import net.minecraft.entity.EnumCreatureType
import net.minecraft.util.{BlockPos, IProgressUpdate}
import net.minecraft.world.biome.BiomeGenBase.SpawnListEntry
import net.minecraft.world.chunk.storage.{AnvilChunkLoader, IChunkLoader}
import net.minecraft.world.chunk.{Chunk, EmptyChunk, IChunkProvider}
import net.minecraft.world.{ChunkCoordIntPair, World}

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
  * Created by EJ on 7/20/2016.
  */
object ChunkProviderShip {
  val ShipChunkIO = new ShipChunkIOExecutor
}
class ChunkProviderShip(shipWorld: ShipWorld, chunkLoader: IChunkLoader) extends IChunkProvider {

  val ChunkMap = mutable.Map[ChunkCoordIntPair,Chunk]()
  val LoadedChunks = mutable.ListBuffer[Chunk]()
  // This is all the chunks that are going to be unloaded
  val DroppedChunks = mutable.Set[ChunkCoordIntPair]()
  val ShipWorld = shipWorld
  val ChunkLoader = chunkLoader
  val DummyChunk = new EmptyChunk(shipWorld,0,0)

  override def canSave: Boolean = true

  override def makeString(): String = s"ShipWorldChunkCache: $ChunkMap"

  override def getLoadedChunkCount: Int = ChunkMap.size

  override def saveExtraData(): Unit = {}

  override def saveChunks(saveExtraData : Boolean, progressCallback: IProgressUpdate): Boolean = {
    // Don't entirely know what this does
    var i = 0
    LoadedChunks.foreach(chunk => {
      if (saveExtraData)
        saveChunkExtraData(chunk)
      if (chunk.needsSaving(saveExtraData)) {
        saveChunkData(chunk)
        chunk.setModified(false)
        i += 1

        if (i == 24 && !saveExtraData)
          return false
      }
    })
    true
  }

  // I think this is just related to populating a chunk with ore, so we can safely ignore it
  override def func_177460_a(p_177460_1_ : IChunkProvider, p_177460_2_ : Chunk, p_177460_3_ : Int, p_177460_4_ : Int): Boolean = false

  override def chunkExists(x: Int, z: Int): Boolean = ChunkMap.get(new ChunkCoordIntPair(x,z)).isDefined
  // Animals wont spawn on the ship
  override def getPossibleCreatures(creatureType: EnumCreatureType, pos: BlockPos): util.List[SpawnListEntry] = new util.ArrayList[SpawnListEntry]()
  // Do not populate the chunk with ores
  override def populate(p_73153_1_ : IChunkProvider, p_73153_2_ : Int, p_73153_3_ : Int): Unit = {}
  // Ships don't have (game-generated) structures
  override def recreateStructures(p_180514_1_ : Chunk, p_180514_2_ : Int, p_180514_3_ : Int): Unit = {}
  // Ships don't have strongholds
  override def getStrongholdGen(worldIn: World, structureName: String, position: BlockPos): BlockPos = null

  override def provideChunk(x: Int, z: Int): Chunk = {
    val foundChunk = ChunkMap.get(new ChunkCoordIntPair(x,z))
    if (foundChunk.isDefined)
      foundChunk.get
    else {
      if (shipWorld.ChunksOnShip.contains(new ChunkCoordIntPair(x,z)))
        loadChunk(x,z)
      else
        DummyChunk
    }

  }

  override def provideChunk(blockPosIn: BlockPos): Chunk = provideChunk(blockPosIn.getX >> 4, blockPosIn.getZ >> 4)

  def loadChunk(x: Int, z: Int):Chunk = loadChunk(x, z, null)

  def loadChunk(x:Int, z:Int, runnable:Runnable): Chunk = {
    val chunkCoords = new ChunkCoordIntPair(x,z)
    DroppedChunks.remove(chunkCoords)
    var chunk: Chunk = ChunkMap.get(chunkCoords).orNull
    var loader: AnvilChunkLoader = null

    this.ChunkLoader match {
      case loader1: AnvilChunkLoader => loader = loader1
      case _ => loader = null
    }

    if (chunk == null && loader != null && loader.chunkExists(shipWorld,x,z)) {
      if (runnable != null) {
        ChunkProviderShip.ShipChunkIO.queueChunkLoad(ShipWorld,loader,this,x,z,runnable)
        return null
      }
      else
        chunk = ChunkProviderShip.ShipChunkIO.syncChunkLoad(ShipWorld,loader,this,x,z)
    }
    else
    {
      val newChunk = new Chunk(shipWorld,x,z)
      ChunkMap(new ChunkCoordIntPair(x,z)) = newChunk
      LoadedChunks.append(newChunk)
      newChunk.onChunkLoad()
      chunk = newChunk
    }
    if (runnable != null)
      runnable.run()

    chunk
  }


  def unloadAllChunks() = LoadedChunks.foreach(c => DroppedChunks.add(new ChunkCoordIntPair(c.xPosition,c.zPosition)))

  def dropChunk(x:Int,z:Int):Unit = {
    DroppedChunks.add(new ChunkCoordIntPair(x,z))
  }

  override def unloadQueuedChunks(): Boolean = {
    if (!canSave) return false
    // Don't unload forced chunks
    shipWorld.getPersistentChunks.entries().foreach(e => DroppedChunks.remove(e.getKey))
    for (i <- 0 until 100) {
      if (DroppedChunks.nonEmpty) {
        val key = DroppedChunks.head
        val Chunk = ChunkMap.get(key)

        if (Chunk.isDefined) {
          Chunk.get.onChunkUnload()
          saveChunkData(Chunk.get)
          saveChunkData(Chunk.get)
          ChunkMap.remove(key)
          LoadedChunks.remove(key)
          DroppedChunks.remove(key)
        }
      }
    }
    true
  }
  def saveChunkData(chunk: Chunk): Unit = chunkLoader.saveChunk(shipWorld,chunk)
  def saveChunkExtraData(chunk: Chunk): Unit = chunkLoader.saveExtraChunkData(shipWorld,chunk)

}
