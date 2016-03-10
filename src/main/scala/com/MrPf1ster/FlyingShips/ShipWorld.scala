package com.MrPf1ster.FlyingShips

import java.lang.Math._

import com.MrPf1ster.FlyingShips.entities.ShipEntity
import net.minecraft.block.Block
import net.minecraft.entity.EntityHanging
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{AxisAlignedBB, BlockPos, ITickable}
import net.minecraft.world.World


/**
  * Created by EJ on 3/2/2016.
  */
class ShipWorld(originWorld:World, originPos:BlockPos, blockSet:Set[BlockPos],ship:ShipEntity ) extends DetachedWorld(originWorld,"Ship") {
  // Write blocks to world



  var BlockStore = new BlocksStorage()
  BlockStore.loadFromWorld(originWorld,originPos,blockSet)
  val BlockSet = BlockStore.getBlockMap.keys.toSet
  val OriginPos = originPos
  val Ship = ship
  val BiomeID = OriginWorld.getBiomeGenForCoords(Ship.ShipBlockPos).biomeID

  def genTileEntities: Set[TileEntity] = {
    BlockSet
      .filter(pos => OriginWorld.getTileEntity(Ship.getWorldPos(pos)) != null)
      .map(tileEntityPos => {
        def relativePosition = tileEntityPos
        def tileEntity = OriginWorld.getTileEntity(Ship.getWorldPos(relativePosition))
        var copyTileEntity:TileEntity = null
        try {
          val nbt = new NBTTagCompound
          tileEntity.writeToNBT(nbt)
          copyTileEntity = TileEntity.createAndLoadEntity(nbt)

          copyTileEntity.setWorldObj(this)
          copyTileEntity.setPos(relativePosition)
          copyTileEntity.validate()
          setTileEntity(copyTileEntity.getPos, copyTileEntity)
        }
        catch {
          case ex: Exception => println(s"There was an error moving TileEntity ${tileEntity.getClass.getName} at $tileEntityPos") // Error reporting
        }
        copyTileEntity // Return our copied to ship tile entity for the map function
      })
  }

  var TileEntities: Set[TileEntity] = genTileEntities


  // Go away ;-;
  val HangingEntities : Set[EntityHanging] = null


  def genBoundingBox() = {
    val relative = genRelativeBoundingBox()
    val minWorldPos = Ship.getWorldPos(new BlockPos(relative.minX, relative.minY, relative.minZ))
    val maxWorldPos = Ship.getWorldPos(new BlockPos(relative.maxX, relative.maxY, relative.maxZ))
    new AxisAlignedBB(minWorldPos, maxWorldPos)
  }

  def genRelativeBoundingBox() = {
    if (this.Ship.isDead) {
      new AxisAlignedBB(0, 0, 0, 0, 0, 0)
    }
    else {
      var minX = Int.MaxValue
      var minY = Int.MaxValue
      var minZ = Int.MaxValue
      var maxX = Int.MinValue
      var maxY = Int.MinValue
      var maxZ = Int.MinValue

      BlockSet.foreach(pos => {
        minX = min(minX, pos.getX)
        minY = min(minY, pos.getY)
        minZ = min(minZ, pos.getZ)

        maxX = max(maxX, pos.getX)
        maxY = max(maxY, pos.getY)
        maxZ = max(maxZ, pos.getZ)


      })

      val minPos = new BlockPos(minX, minY, minZ)
      val maxPos = new BlockPos(maxX + 1, maxY + 1, maxZ + 1)

      new AxisAlignedBB(minPos, maxPos)
    }
  }

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

  override def getClosestPlayer(x: Double, y: Double, z: Double, distance: Double) = {
    val worldPos = Ship.getWorldPos(new BlockPos(x, y, z))
    val returnthing = super.getClosestPlayer(worldPos.getX, worldPos.getY, worldPos.getZ, distance)
    println(returnthing)
    returnthing
  }

  override def updateEntities() = {
    TileEntities
      .map(te => te.asInstanceOf[ITickable])
      .foreach(te => te.update())
  }


  def isValid = !BlockSet.isEmpty
  def needsRenderUpdate = false // TODO: Implement later


}
