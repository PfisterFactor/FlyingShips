package com.MrPf1ster.FlyingShips

import com.MrPf1ster.FlyingShips.entities.ShipEntity
import net.minecraft.block.Block
import net.minecraft.entity.EntityHanging
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{BlockPos, Vec3i}
import net.minecraft.world.{EnumSkyBlock, World}


/**
  * Created by EJ on 3/2/2016.
  */
class ShipWorld(originWorld:World, originPos:BlockPos, blockSet:Set[BlockPos],ship:ShipEntity ) extends DetachedWorld(originWorld,"Ship") {
  // Write blocks to world



  var BlockStore = new BlocksStorage()
  BlockStore.loadFromWorld(originWorld,originPos,blockSet)
  val BlockSet = BlockStore.getBlockMap.keys.toSet
  val OriginPos = originPos
  val OriginWorld = originWorld
  val Ship = ship

  val BiomeID = OriginWorld.getBiomeGenForCoords(OriginPos).biomeID

  // THIS MOST LIKELY DOES NOT WORK
  val TileEntities : Set[TileEntity] = {
    BlockSet
      .filter(blockPos => {
        OriginWorld.getTileEntity(getWorldPos(blockPos)) != null
      }) // Get rid of non TileEntities
      .map(blockPos => { // Create copy of all tile entities
    val tileEntity = OriginWorld.getTileEntity(getWorldPos(blockPos))
        def tileEntityPos = tileEntity.getPos
        val relativePosition = new BlockPos(tileEntityPos.getX - OriginPos.getX, tileEntityPos.getY - OriginPos.getY , tileEntityPos.getZ - OriginPos.getZ )
        var copyTileEntity:TileEntity = null
        try {
          val nbt = new NBTTagCompound
          tileEntity.writeToNBT(nbt)
          copyTileEntity = TileEntity.createAndLoadEntity(nbt)

          copyTileEntity.setWorldObj(this)
          copyTileEntity.setPos(relativePosition)
          copyTileEntity.validate()
        }
        catch {
          case ex: Exception => println(s"There was an error moving TileEntity ${tileEntity.getClass.getName} at $tileEntityPos") // Error reporting
        }
        copyTileEntity // Return our copied to ship tile entity for the map function

      })
  }
  // Go away ;-;

  val HangingEntities : Set[EntityHanging] = null
  override def getBlockState(pos:BlockPos) = {
    val got = BlockStore.getBlock(pos)
    if (got.isDefined)
      got.get.BlockState // Got get got get got get
    else
      Block.getStateById(0)

  }
  override def getTileEntity(pos:BlockPos) = {
    TileEntities.find(x => {
      x.getPos == pos
    }).orNull
  }

  override def getLightFromNeighbors(pos: BlockPos) = 15
  override def getLightFromNeighborsFor(typ: EnumSkyBlock, pos:BlockPos) = {
    if (typ == EnumSkyBlock.SKY)
      15
    else
      4
  }
  //override def getCombinedLight(pos:BlockPos,lightValue:Int) = 0

  def isValid = !BlockSet.isEmpty
  def needsRenderUpdate = false // TODO: Implement later
  def getWorldPos(pos: BlockPos) = OriginPos.add(pos.getX, pos.getY, pos.getZ)

  def getRelativePos(pos: BlockPos) = pos.subtract(new Vec3i(OriginPos.getX, OriginPos.getY, OriginPos.getZ))

}
