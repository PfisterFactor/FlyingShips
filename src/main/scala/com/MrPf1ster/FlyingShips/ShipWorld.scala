package com.MrPf1ster.FlyingShips

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import javax.vecmath.Quat4f

import com.MrPf1ster.FlyingShips.entities.EntityShip
import com.MrPf1ster.FlyingShips.network.BlocksChangedMessage
import com.MrPf1ster.FlyingShips.util.{UnifiedPos, VectorUtils}
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityHanging
import net.minecraft.nbt.{CompressedStreamTools, NBTTagCompound}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.{BlockPos, ITickable, MovingObjectPosition, Vec3}
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint

import scala.collection.mutable.{Map => mMap, Set => mSet}


/**
  * Created by EJ on 3/2/2016.
  */
class ShipWorld(originWorld: World, blocks: Set[UnifiedPos], ship: EntityShip) extends DetachedWorld(originWorld, "Ship") {


  val OriginWorld = originWorld

  val Ship = ship

  // The coordinates of the ship block in the origin world. Conveniently the EntityShip's position
  def OriginPos() = Ship.getPosition

  def BlockSet = BlockStore.getBlockMap.keys.toSet

  // Stores all the blocks on the ship (not tile entities!)
  val BlockStore = new BlocksStorage(this) {
    loadFromWorld(OriginWorld, blocks)
  }

  // The Shipblock
  def ShipBlock = getBlockState(new BlockPos(0,0,0))

  // TODO: Change this to be the biome directly under the ship
  val BiomeID = OriginWorld.getBiomeGenForCoords(OriginPos).biomeID

  // All TileEntities on Ship, mapped with a Unified Pos
  var TileEntities: Map[UnifiedPos, TileEntity] = moveTileEntitiesOntoShip

  // All HangingEntities on ship, mapped with a Unified Pos
  var HangingEntities: Map[UnifiedPos, EntityHanging] = null

  private val ChangedBlocks: mSet[UnifiedPos] = mSet()
  private var doRenderUpdate = false



  private def moveTileEntitiesOntoShip: Map[UnifiedPos, TileEntity] = {
    if (!isValid)
      return Map()

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


  override def getBlockState(pos:BlockPos) = {
    val got = BlockStore.getBlock(pos)
    if (got.isDefined)
      got.get.BlockState // Got get got get got get
    else
      Block.getStateById(0)

  }

  override def getTileEntity(pos: BlockPos) = TileEntities.get(new UnifiedPos(pos, OriginPos, true)).orNull

  override def addTileEntity(te: TileEntity): Boolean = {
    if (te.isInvalid) return false
    TileEntities = TileEntities + (UnifiedPos(te.getPos,OriginPos,true) -> te)
    true
  }


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
      return true
    }
    false
  }

  def applyBlockChange(pos: BlockPos, newState: IBlockState, flags: Int): Boolean = {
    val storage: Option[BlockStorage] = BlockStore.getBlock(pos)
    if (storage.isEmpty) {
      BlockStore.setBlock(pos,newState)
    }

    storage.get.BlockState = newState
    val TE = TileEntities.get(new UnifiedPos(pos, Ship.getPosition, true))
    if (TE.isDefined)
      TE.get.updateContainingBlockInfo()

    doRenderUpdate = true
    true
  }

  def onShipMove() = {doRenderUpdate = true}

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

  def getWorldData: (Array[Byte], Array[Byte]) = {
    // Block Data
    val blockData = BlockStore.getByteData


    // Tile Entities
    val bytes = new ByteArrayOutputStream()
    val out = new DataOutputStream(bytes)

    out.writeInt(TileEntities.size)
    TileEntities.foreach(pair => {
      def uPos = pair._1
      def te = pair._2
      val nbt = new NBTTagCompound()
      te.writeToNBT( nbt )
      CompressedStreamTools.writeCompressed(nbt,out)
    })
    out.close()

    def tileEntData = bytes.toByteArray

    (blockData,tileEntData)
  }

  def setWorldData(blockData:Array[Byte],tileEntData:Array[Byte]) = {
    // Block Data
    BlockStore.writeByteData(blockData)

    val bytes = new ByteArrayInputStream(tileEntData)
    val in = new DataInputStream(bytes)

    val teSize = in.readInt()

    val tileentities = new Array[TileEntity](teSize)
    for (i <- 0 until teSize)
      tileentities(i) = TileEntity.createAndLoadEntity(CompressedStreamTools.readCompressed(in))

    // Map tile entity positions to UnifiedPositions and then zip it with the tile entities array
    TileEntities = tileentities.map(te => UnifiedPos(te.getPos,OriginPos,true)).zip(tileentities).toMap
  }

  def isValid = BlockStore.nonEmpty

  def needsRenderUpdate() = doRenderUpdate

  def onRenderUpdate() = {doRenderUpdate = false}



  override def getBiomeGenForCoords(pos: BlockPos) = OriginWorld.getBiomeGenForCoords(Ship.getPosition.add(pos))

  override def getBiomeGenForCoordsBody(pos: BlockPos) = OriginWorld.getBiomeGenForCoords(Ship.getPosition.add(pos))


}
