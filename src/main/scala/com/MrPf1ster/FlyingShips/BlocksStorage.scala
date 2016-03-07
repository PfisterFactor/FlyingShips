package com.MrPf1ster.FlyingShips


import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraft.world.World

import scala.collection.mutable.{Map => mMap}

/**
  * Created by EJ on 3/3/2016.
  */
class BlocksStorage() {
  private var BlockMap: mMap[BlockPos,BlockStorage] = mMap()


  def getBlockMap = BlockMap
  def setBlock(pos:BlockPos,state:IBlockState) = BlockMap.put(pos,new BlockStorage(state))
  def getBlock(pos:BlockPos) : Option[BlockStorage] = BlockMap.get(pos)

  def loadFromWorld(world:World,origin:BlockPos,blockSet: Set[BlockPos]) = {
    blockSet.foreach(blockPos => {
      val blockStorage = new BlockStorage()
      blockStorage.readFromWorld(world,blockPos)

      val relativePosition = new BlockPos(blockPos.getX - origin.getX,blockPos.getY - origin.getY,blockPos.getZ - origin.getZ)

      BlockMap.put(relativePosition,blockStorage)

    })
  }
  def writeToWorld(world:World, blockCorrespondence:Map[BlockPos,BlockPos]) = {
    blockCorrespondence.foreach[Unit](correspondence => {
      val shipPos = correspondence._1
      val worldPos = correspondence._2
      val storage = getBlock(shipPos)
      if (storage.isDefined)
        storage.get.writeToWorld(world,worldPos)
    })
  }



}
