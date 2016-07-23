package mrpf1ster.flyingships.world

import mrpf1ster.flyingships.util.UnifiedPos
import net.minecraft.block.BlockAir
import net.minecraft.block.state.IBlockState
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



}
