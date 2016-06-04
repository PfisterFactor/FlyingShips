package com.MrPf1ster.FlyingShips.util

import net.minecraft.util.{BlockPos, EnumFacing}
import net.minecraft.world.World

import scala.collection.mutable.{Queue => mQueue, Set => mSet}


/**
  * Created by EJ on 2/20/2016.
  */
object BlockUtils {
  def loadInClasses = {
    def loader = ClassLoader.getSystemClassLoader
    def className(a: Any): String = a.getClass.getName

    // Loads in Scala's predefined classes and methods
    // Implemented cause it caused a cpu stutter when ShipCreatorBlock was first used
    loader.loadClass(className(scala.Predef))


    loader.loadClass(className(mQueue))
    loader.loadClass(className(mSet))
  }


  // findAllBlocksConnected(world,pos)
  // Uses a flood fill algorithm to go through all the blocks connected to one another and adds them into an array
  // Used for creating the ship
  def findAllBlocksConnected(world: World, pos: BlockPos): Set[BlockPos] = {
    // Not very functional-like implementation of flood fill
    // Our block queue: if done recursively the method would take too much stack memory
    val blockQueue = mQueue[(BlockPos, Option[EnumFacing])]()
    // Set that stores blocks apart of the ship
    val blockSet = mSet[BlockPos]()

    // Enqueue the first block, aka the ship controller block, for processing
    blockQueue.enqueue((pos, None)) // Starting direction is null
    while (blockQueue.nonEmpty && blockSet.size < 1000) {
      // Max amount of blocks hardcoded for now, TODO: Move to config file
      // Get the next blockSet in queue
      val currentBlock = blockQueue.dequeue()
      // Air can't be apart of the ship for obvious reasons, TODO: Make a block blacklist
      if (!world.isAirBlock(currentBlock._1)) {
        // If we found a ship helm block, then set it to true

        if (blockSet.add(currentBlock._1)) {
          // Mark it as a block on the ship, because it is a Set it ignores duplicates
          EnumFacing.values.filter(side => {
            currentBlock._2.getOrElse(None) != side.getOpposite
          }).foreach(side => {
            blockQueue.enqueue((currentBlock._1.offset(side), Some(side))) // Queue up the blocks around it
          })
        }

      }
    }
    // Return our blocks as a set
    blockSet.toSet
  }

}
