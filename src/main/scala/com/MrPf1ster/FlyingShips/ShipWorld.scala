package com.MrPf1ster.FlyingShips

import javax.vecmath.Quat4f

import com.MrPf1ster.FlyingShips.entities.ShipEntity
import com.MrPf1ster.FlyingShips.network.BlocksChangedMessage
import com.MrPf1ster.FlyingShips.util.{UnifiedPos, VectorUtils}
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityHanging
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.{BlockPos, ITickable, MovingObjectPosition, Vec3}
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint

import scala.collection.mutable.{Set => mSet}


/**
  * Created by EJ on 3/2/2016.
  */
class ShipWorld(originWorld: World, originPos: BlockPos, blockSet: Set[BlockPos], ship: ShipEntity) extends DetachedWorld(originWorld, "Ship") {

  val OriginPos = originPos
  val Ship = ship
  val ShipBlock = originWorld.getBlockState(originPos)

  var BlockStore = new BlocksStorage(this)
  BlockStore.loadFromWorld(originWorld, originPos, blockSet)
  val BlockSet = blockSet.map(pos => UnifiedPos(pos, Ship.getPosition, false))
  val BiomeID = OriginWorld.getBiomeGenForCoords(OriginPos).biomeID
  private val ChangedBlocks: mSet[UnifiedPos] = mSet() // TODO: Figure out what this is
  private var doRenderUpdate = false

  private def genTileEntities: Map[UnifiedPos, TileEntity] = {
    if (!isValid) {
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


  override def getBlockState(pos:BlockPos) = {
    val got = BlockStore.getBlock(pos)
    if (got.isDefined)
      got.get.BlockState // Got get got get got get
    else
      Block.getStateById(0)

  }

  override def getTileEntity(pos: BlockPos) = TileEntities.get(new UnifiedPos(pos, Ship.getPosition, true)).orNull


  override def updateEntities(): Unit = {
    if (!isValid)
      return
    TileEntities
      .foreach(pair => {
        def te = pair._2
        def uPos = pair._1
        te.asInstanceOf[ITickable].update()
      })

    if (!isRemote) {
      pushBlockChangesToClient()
    }
  }

  def pushBlockChangesToClient(): Unit = {
    if (!isValid) return
    if (Ship == null) return
    if (ChangedBlocks.isEmpty) return

    val message = new BlocksChangedMessage(Ship, ChangedBlocks.map(pos => pos.RelativePos).toArray)
    val targetPoint = new TargetPoint(OriginWorld.provider.getDimensionId, Ship.getPosition.getX, Ship.getPosition.getY, Ship.getPosition.getZ, 64)
    FlyingShips.flyingShipPacketHandler.INSTANCE.sendToAllAround(message, targetPoint)

    ChangedBlocks.clear()

  }

  override def setBlockState(pos: BlockPos, newState: IBlockState): Boolean = {
    setBlockState(pos, newState, 3)
  }

  override def setBlockState(pos: BlockPos, newState: IBlockState, flags: Int): Boolean = {
    if (applyBlockChange(pos, newState, flags) && this.isValid) {
      if (!isRemote)
        ChangedBlocks.add(new UnifiedPos(pos, Ship.getPosition, true))
      true
    }
    false
  }

  def applyBlockChange(pos: BlockPos, newState: IBlockState, flags: Int): Boolean = {
    val storage: Option[BlockStorage] = BlockStore.getBlock(pos)
    if (storage.isEmpty) return false

    storage.get.BlockState = newState
    val TE = TileEntities.get(new UnifiedPos(pos, Ship.getPosition, true))
    if (TE.isDefined)
      TE.get.updateContainingBlockInfo()

    doRenderUpdate = true
    true
  }

  // Ray traces blocks on ship, arguments are non-relative
  // It rotates the look and ray vector against the ship's current rotation so we can use Minecraft's built in world block ray-trace
  def rayTrace(start:Vec3, end:Vec3): Option[MovingObjectPosition] = {

    // Gets the opposite rotation of our entity
    val inversedRot: Quat4f = Ship.Rotation.clone().asInstanceOf[Quat4f] // clone because inverse mutates
    inversedRot.inverse()

    val relativeStart = start.subtract(Ship.getPositionVector.addVector(0.5,0.5,0.5))
    val relativeEnd = end.subtract(Ship.getPositionVector.addVector(0.5,0.5,0.5))

    // The eye position and ray, rotated by the ship block's center, to the opposite of the ships current rotation
    val rotatedStart = VectorUtils.rotatePointByQuaternion(relativeStart,inversedRot).addVector(0.5,0.5,0.5)
    val rotatedEnd = VectorUtils.rotatePointByQuaternion(relativeEnd,inversedRot).addVector(0.5,0.5,0.5)

    // The result of the ray-trace on the ship world
    val rayTrace = Ship.ShipWorld.rayTraceBlocks(rotatedStart, rotatedEnd)

    if (rayTrace != null && rayTrace.typeOfHit == MovingObjectType.BLOCK)
      Some(rayTrace)
    else
      None

  }

  def isValid = BlockSet.nonEmpty

  def needsRenderUpdate() = {
    val a = doRenderUpdate
    doRenderUpdate = false
    a
  }


  override def getBiomeGenForCoords(pos: BlockPos) = OriginWorld.getBiomeGenForCoords(Ship.getPosition.add(pos))

  override def getBiomeGenForCoordsBody(pos: BlockPos) = OriginWorld.getBiomeGenForCoords(Ship.getPosition.add(pos))


}
