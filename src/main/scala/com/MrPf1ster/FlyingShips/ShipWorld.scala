package com.MrPf1ster.FlyingShips

import java.lang.Math._

import com.MrPf1ster.FlyingShips.entities.ShipEntity
import com.MrPf1ster.FlyingShips.util.UnifiedPos
import net.minecraft.block.Block
import net.minecraft.entity.EntityHanging
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{AxisAlignedBB, BlockPos, ITickable}
import net.minecraft.world.World

import scala.collection.mutable.{Set => mSet}


/**
  * Created by EJ on 3/2/2016.
  */
class ShipWorld(originWorld: World, originPos: BlockPos, blockSet: Set[BlockPos], ship: ShipEntity) extends DetachedWorld(originWorld, "Ship") {
  // Write blocks to world


  val OriginPos = originPos
  val Ship = ship

  var BlockStore = new BlocksStorage(this)
  BlockStore.loadFromWorld(originWorld, originPos, blockSet)
  val BlockSet = BlockStore.getBlockMap.keys.map(pos => new UnifiedPos(pos, Ship, true)).toSet
  val BiomeID = OriginWorld.getBiomeGenForCoords(Ship.ShipBlockPos).biomeID

  def genTileEntities: Map[UnifiedPos, TileEntity] = {
    if (this.Ship.isDead) {
      return Map()
    }
    BlockSet
      .filter(uPos => OriginWorld.getTileEntity(uPos.WorldPos) != null)
      .map(tileEntityUPos => {
        def tileEntity = OriginWorld.getTileEntity(tileEntityUPos.WorldPos)
        var copyTileEntity:TileEntity = null
        try {
          val nbt = new NBTTagCompound
          tileEntity.writeToNBT(nbt)
          copyTileEntity = TileEntity.createAndLoadEntity(nbt)

          copyTileEntity.setWorldObj(this)
          copyTileEntity.setPos(tileEntityUPos.RelativePos)
          copyTileEntity.validate()
          setTileEntity(copyTileEntity.getPos, copyTileEntity)
        }
        catch {
          case ex: Exception => println(s"There was an error moving TileEntity ${tileEntity.getClass.getName} at ${tileEntityUPos.WorldPos}") // Error reporting
        }
        tileEntityUPos -> copyTileEntity // Return our copied to ship tile entity for the map function
      }).toMap
  }

  var TileEntities: Map[UnifiedPos, TileEntity] = genTileEntities




  // Go away ;-;
  val HangingEntities: mSet[EntityHanging] = null


  def genBoundingBox() = {
    val relative = genRelativeBoundingBox()
    val uMinPos = new UnifiedPos(relative.minX, relative.minY, relative.minZ, Ship, true)
    val uMaxPos = new UnifiedPos(relative.maxX, relative.maxY, relative.maxZ, Ship, true)
    new AxisAlignedBB(uMinPos.WorldPos, uMaxPos.WorldPos)
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

      BlockSet.foreach(uPos => {
        minX = min(minX, uPos.RelPosX)
        minY = min(minY, uPos.RelPosY)
        minZ = min(minZ, uPos.RelPosZ)

        maxX = max(maxX, uPos.RelPosX)
        maxY = max(maxY, uPos.RelPosY)
        maxZ = max(maxZ, uPos.RelPosZ)


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

  override def getTileEntity(pos: BlockPos) = TileEntities(new UnifiedPos(pos, Ship, true))

  override def updateEntities() = {
    TileEntities
      .foreach(pair => {
        def te = pair._2
        def uPos = pair._1
        te.setPos(uPos.WorldPos)
        te.asInstanceOf[ITickable].update()
        te.setPos(uPos.RelativePos)
      })
  }


  def isValid = BlockSet.nonEmpty
  def needsRenderUpdate = false // TODO: Implement later


}
