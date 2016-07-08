package mrpf1ster.flyingships.world

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

import mrpf1ster.flyingships.util.UnifiedPos
import net.minecraft.block.state.IBlockState
import net.minecraft.block.{Block, BlockAir}
import net.minecraft.util.BlockPos
import net.minecraft.world.World

import scala.collection.mutable.{Map => mMap}

/**
  * Created by EJ on 3/3/2016.
  */
class BlocksStorage(ShipWorld: ShipWorld) {

  private var BlockMap: mMap[UnifiedPos,BlockStorage] = mMap()


  def getBlockMap = BlockMap

  def setBlock(pos:BlockPos,state:IBlockState): Unit = setBlock(UnifiedPos(pos,ShipWorld.OriginPos,IsRelative = true),state)
  def setBlock(pos:UnifiedPos,state:IBlockState): Unit = {
    if (state.getBlock.isInstanceOf[BlockAir])
      BlockMap.remove(pos)
    else
      BlockMap.put(pos,new BlockStorage(state))
  }

  def getBlock(pos:BlockPos) : Option[BlockStorage] = BlockMap.get(UnifiedPos(pos,ShipWorld.OriginPos,IsRelative = true))
  def getBlock(pos:UnifiedPos) : Option[BlockStorage] = BlockMap.get(pos)

  def isEmpty:Boolean = BlockMap.isEmpty
  def nonEmpty:Boolean = BlockMap.nonEmpty

  def loadFromWorld(world:World,blockSet: Set[UnifiedPos]) = {
    blockSet.foreach(uPos => {
      val blockStorage = new BlockStorage()
      blockStorage.readFromWorld(world,uPos)
      BlockMap.put(uPos,blockStorage)

    })
  }
  def writeToWorld(world:World, positions:Set[UnifiedPos]) = {
    positions.foreach[Unit](uPos => {
      val storage = getBlock(uPos)
      if (storage.isDefined)
        storage.get.writeToWorld(world,uPos)
    })
  }


  def getByteData: Array[Byte] = {
    val bytes = new ByteArrayOutputStream()
    val out = new DataOutputStream(bytes)


    // Length of map
    out.writeInt(BlockMap.size)

    // BlockPos
    BlockMap.keys.foreach(pos => out.writeLong(pos.RelativePos.toLong))

    // BlockStorage
    //noinspection ScalaDeprecation
    BlockMap.values.foreach(state => out.writeInt(Block.BLOCK_STATE_IDS.get(state.BlockState)))



    out.close()

    bytes.toByteArray

  }

  def writeByteData(bytes:Array[Byte]) = {
    val byteStream = new ByteArrayInputStream(bytes)
    val in = new DataInputStream(byteStream)

    // Length of map
    val mapLength = in.readInt()

    // BlockPos
    val blockpositions = new Array[UnifiedPos](mapLength)
    for (i:Int <- 0 until mapLength)
      blockpositions(i) = UnifiedPos(BlockPos.fromLong(in.readLong()),ShipWorld.OriginPos,IsRelative = true)

    // Block Storage
    val blockstorages = new Array[BlockStorage](mapLength)
    //noinspection ScalaDeprecation
    for (i <- 0 until mapLength)
      blockstorages(i) = new BlockStorage(Block.BLOCK_STATE_IDS.getByValue(in.readInt()))

    in.close()

    // Zips the two arrays into a map
    BlockMap = mMap(blockpositions.zip(blockstorages).toSeq:_*)
  }


}