package com.MrPf1ster.FlyingShips

import com.MrPf1ster.FlyingShips.util.UnifiedPos
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraft.world.World


/**
  * Created by EJ on 3/3/2016.
  */
class BlockStorage(block:IBlockState) {
  def this() = this(Block.getStateById(0))

  var BlockState = block

  def readFromWorld(world:World,pos:BlockPos) = {
    BlockState = world.getBlockState(pos)
  }

  def readFromWorld(world:World,uPos:UnifiedPos) = {
    BlockState = world.getBlockState(uPos.WorldPos)
  }

  def writeToWorld(world:World,pos:BlockPos) = {
    world.setBlockState(pos,BlockState)
  }

  def writeToWorld(world:World,uPos:UnifiedPos) = {
    world.setBlockState(uPos.WorldPos,BlockState)
  }

}
