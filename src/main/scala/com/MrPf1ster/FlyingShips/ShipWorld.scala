package com.MrPf1ster.FlyingShips

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import javax.vecmath.Quat4f

import com.MrPf1ster.FlyingShips.entities.EntityShip
import com.MrPf1ster.FlyingShips.network.BlocksChangedMessage
import com.MrPf1ster.FlyingShips.util.{UnifiedPos, UnifiedVec, VectorUtils}
import net.minecraft.block.state.IBlockState
import net.minecraft.block.{Block, BlockAir}
import net.minecraft.entity.{Entity, EntityHanging}
import net.minecraft.nbt.{CompressedStreamTools, NBTTagCompound}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util._
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint

import scala.collection.mutable.{Map => mMap, Set => mSet}


/**
  * Created by EJ on 3/2/2016.
  */
// TODO: Make
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

  // The Ship Block
  def ShipBlock = getBlockState(new BlockPos(0,0,0))

  // TODO: Change this to be the biome directly under the ship
  val BiomeID = OriginWorld.getBiomeGenForCoords(OriginPos).biomeID

  // All TileEntities on Ship, mapped with a Unified Pos
  var TileEntities: Map[UnifiedPos, TileEntity] = moveTileEntitiesOntoShip
  def TickableTileEntities: Map[UnifiedPos, TileEntity] = TileEntities.filter(tuple => tuple._2.isInstanceOf[ITickable])

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

  override def checkNoEntityCollision(aabb:AxisAlignedBB):Boolean = checkNoEntityCollision(aabb,null)

  override def checkNoEntityCollision(aabb:AxisAlignedBB, entity:Entity):Boolean = {
    // Todo: Implement this
    return true
  }

  override def getTileEntity(pos: BlockPos) = TileEntities.get(new UnifiedPos(pos, OriginPos, true)).orNull
  override def setTileEntity(pos:BlockPos,te:TileEntity) = {
    if (te != null)
      TileEntities = TileEntities + (UnifiedPos(pos,OriginPos,true) -> te)
  }

  override def addTileEntity(te: TileEntity): Boolean = {
    if (te.isInvalid) return false
    TileEntities = TileEntities + (UnifiedPos(te.getPos,OriginPos,true) -> te)
    true
  }


  override def updateEntities(): Unit = {
    if (!isValid)
      return
    TickableTileEntities
      .foreach(pair => {
        def uPos = pair._1
        def te = pair._2
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
    if (isRemote) return false

    if (applyBlockChange(pos, newState, flags) && this.isValid) {
        ChangedBlocks.add(new UnifiedPos(pos, Ship.getPosition, true))
        pushBlockChangesToClient()
        return true
    }
    false
  }

  def applyBlockChange(pos: BlockPos, newState: IBlockState, flags: Int): Boolean = {
    if (pos == new BlockPos(0,0,0)) return false

    val storage: Option[BlockStorage] = BlockStore.getBlock(pos)

    /*
    if (newState.isInstanceOf[BlockAir] && storage.isDefined)
      storage.get.BlockState.getBlock.onBlockHarvested(this,pos,storage.get.BlockState,player)
    */

    BlockStore.setBlock(pos, newState)

    if (storage.isEmpty || newState.getBlock.isInstanceOf[BlockAir])
      Ship.generateBoundingBox




    if (!isRemote && !isAirBlock(pos))
      BlockStore.getBlock(pos).get.BlockState.getBlock.onBlockAdded(this,pos,newState)

    val TE = TileEntities.get(new UnifiedPos(pos, Ship.getPosition, true))
    if (TE.isDefined) {
      TE.get.updateContainingBlockInfo()
      if (isAirBlock(pos)) {
        TE.get.invalidate()
        TileEntities = TileEntities.filterNot(te => te._2 == TE.get)
      }
    }
    else{
      val newTE = newState.getBlock.createTileEntity(this,newState)
      if (newTE != null) {
        newTE.setWorldObj(this)
        newTE.setPos(pos)
        setTileEntity(pos,newTE)
      }

    }

    doRenderUpdate = true
    true
  }

  def onShipMove() = {
    doRenderUpdate = true
  }

  // Ray traces blocks on ship, arguments are non-relative
  // It rotates the look and ray vector against the ship's current rotation so we can use Minecraft's built in world block ray-trace
  def rotatedRayTrace(start:Vec3, end:Vec3): Option[MovingObjectPosition] = {

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

  override def playSoundEffect(x:Double,y:Double,z:Double,soundName:String,volume:Float,pitch:Float) = {
    val newVec = UnifiedVec.convertToWorld(new Vec3(x,y,z),ship.getPositionVector)

    OriginWorld.playSoundEffect(newVec.xCoord,newVec.yCoord,newVec.zCoord,soundName,volume,pitch)
  }

  override def playAuxSFX(par1:Int,pos:BlockPos,par3:Int) = {
    val newPos = UnifiedPos.convertToWorld(pos,ship.getPosition)
    OriginWorld.playAuxSFX(par1,newPos,par3)
  }

  override def isSideSolid(pos:BlockPos,side:EnumFacing,default:Boolean) = getBlockState(pos).getBlock.isSideSolid(this, pos, side)

  override def getBiomeGenForCoords(pos: BlockPos) = OriginWorld.getBiomeGenForCoords(Ship.getPosition.add(pos))

  override def getBiomeGenForCoordsBody(pos: BlockPos) = OriginWorld.getBiomeGenForCoords(Ship.getPosition.add(pos))


}
