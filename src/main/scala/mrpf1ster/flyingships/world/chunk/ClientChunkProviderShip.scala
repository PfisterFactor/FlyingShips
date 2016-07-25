package mrpf1ster.flyingships.world.chunk

import java.util

import mrpf1ster.flyingships.world.ShipWorldClient
import net.minecraft.entity.EnumCreatureType
import net.minecraft.util.{BlockPos, IProgressUpdate}
import net.minecraft.world.biome.BiomeGenBase.SpawnListEntry
import net.minecraft.world.chunk.{Chunk, EmptyChunk, IChunkProvider}
import net.minecraft.world.{ChunkCoordIntPair, World}

import scala.collection.mutable

/**
  * Created by ej on 7/25/16.
  */
case class ClientChunkProviderShip(shipWorldClient: ShipWorldClient) extends IChunkProvider {
  val BlankChunk = new EmptyChunk(shipWorldClient, 0, 0)
  private val ChunkMap: mutable.Map[ChunkCoordIntPair, Chunk] = mutable.Map()
  private val ChunkList: mutable.MutableList[Chunk] = mutable.MutableList()

  override def canSave: Boolean = false

  override def makeString(): String = "ShipClientChunkCache: " + this.ChunkMap.size + ", " + this.ChunkList.size

  override def getLoadedChunkCount: Int = ChunkList.size

  override def saveExtraData(): Unit = {}

  override def saveChunks(p_73151_1_ : Boolean, progressCallback: IProgressUpdate): Boolean = true

  override def func_177460_a(p_177460_1_ : IChunkProvider, p_177460_2_ : Chunk, p_177460_3_ : Int, p_177460_4_ : Int): Boolean = false

  override def chunkExists(x: Int, z: Int): Boolean = true

  override def getPossibleCreatures(creatureType: EnumCreatureType, pos: BlockPos): util.List[SpawnListEntry] = null

  override def populate(p_73153_1_ : IChunkProvider, p_73153_2_ : Int, p_73153_3_ : Int): Unit = {}

  override def recreateStructures(p_180514_1_ : Chunk, p_180514_2_ : Int, p_180514_3_ : Int): Unit = {}

  override def getStrongholdGen(worldIn: World, structureName: String, position: BlockPos): BlockPos = null

  override def provideChunk(x: Int, z: Int): Chunk = {
    val chunk = this.ChunkMap.get(new ChunkCoordIntPair(x, z))
    chunk match {
      case None => BlankChunk
      case _ => chunk.get
    }
  }

  override def provideChunk(blockPosIn: BlockPos): Chunk = provideChunk(blockPosIn.getX >> 4, blockPosIn.getZ >> 4);

  override def unloadQueuedChunks(): Boolean = {
    ChunkList.foreach(_.func_150804_b(false))
    true
  }
}
