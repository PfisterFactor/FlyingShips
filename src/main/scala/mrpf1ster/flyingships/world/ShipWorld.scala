package mrpf1ster.flyingships.world

import java.util
import java.util.Random

import com.google.common.base.Predicate
import io.netty.buffer.Unpooled
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.network.BlocksChangedMessage
import mrpf1ster.flyingships.util.{UnifiedPos, UnifiedVec, VectorUtils}
import net.minecraft.block.state.IBlockState
import net.minecraft.block.{Block, BlockAir}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{Entity, EntityHanging}
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util._
import net.minecraft.world.{World, WorldSettings}
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

import scala.collection.JavaConversions._
import scala.collection.mutable.{Map => mMap, Set => mSet}


/**
  * Created by EJ on 3/2/2016.
  */

class ShipWorld(originWorld: World, blocks: Set[UnifiedPos], ship: EntityShip) extends DetachedWorld(originWorld, "Ship") {


  val OriginWorld = originWorld

  val Ship = ship

  // The coordinates of the ship block in the origin world. Conveniently the EntityShip's position
  def OriginPos() = Ship.getPosition

  def OriginVec() = Ship.getPositionVector

  def BlockSet = BlockStore.getBlockMap.keys.toSet

  // Stores all the blocks on the ship (not tile entities!)
  val BlockStore = new BlocksStorage(this) {
    loadFromWorld(OriginWorld, blocks)
  }

  // The Ship Block's Position
  // TODO: Make the world un-relative to the ship block
  def ShipBlockPos = new BlockPos(0,0,0)

  // The Ship Block
  def ShipBlock = getBlockState(ShipBlockPos)



  // TODO: Change this to be the biome directly under the ship
  val BiomeID = OriginWorld.getBiomeGenForCoords(OriginPos()).biomeID

  // All TileEntities on Ship, mapped with a Unified Pos
  var TileEntities: mMap[UnifiedPos, TileEntity] = moveTileEntitiesOntoShip
  def TickableTileEntities: Map[UnifiedPos, TileEntity] = Map(TileEntities.filter(tuple => tuple._2.isInstanceOf[ITickable]).toSeq:_*)

  // All HangingEntities on ship, mapped with a Unified Pos
  // #Not implemented#
  var HangingEntities: mMap[UnifiedPos, EntityHanging] = null

  private val ChangedBlocks: mSet[UnifiedPos] = mSet()
  private var doRenderUpdate = false



  private def moveTileEntitiesOntoShip: mMap[UnifiedPos, TileEntity] = {
    if (!isValid)
      return mMap()

    mMap(BlockSet
      .filter(uPos => OriginWorld.getTileEntity(uPos.WorldPos) != null)
      .map(tileEntityUPos => {
        val tileEntity = OriginWorld.getTileEntity(tileEntityUPos.WorldPos)
        var copyTileEntity:TileEntity = null
        try {
          val nbt = new NBTTagCompound
          tileEntity.writeToNBT(nbt)
          copyTileEntity = TileEntity.createAndLoadEntity(nbt)

          copyTileEntity.setWorldObj(this)
          copyTileEntity.setPos(tileEntityUPos.RelativePos)
          copyTileEntity.validate()
        }
        catch {
          case ex: Exception => println(s"There was an error moving TileEntity ${tileEntity.getClass.getName} at ${tileEntityUPos.WorldPos}\n$ex") // Error reporting
        }
        tileEntityUPos -> copyTileEntity // Return our copied to ship tile entity for the map function
      }).toSeq:_*)
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
    true
  }

  // Fix for entities on ship later
  override def getEntitiesWithinAABB[T <: Entity](classEntity: Class[_ <: T], bb: AxisAlignedBB): util.List[T] = new util.ArrayList[T]()
  override def getEntitiesWithinAABBExcludingEntity(entityIn: Entity, bb: AxisAlignedBB): util.List[Entity] = new util.ArrayList[Entity]()
  override def getEntitiesWithinAABB[T <: Entity](clazz: Class[_ <: T], aabb: AxisAlignedBB, filter: Predicate[_ >: T]): util.List[T] = new util.ArrayList[T]()

  override def getTileEntity(pos: BlockPos) = TileEntities.get(new UnifiedPos(pos, OriginPos, true)).orNull
  override def setTileEntity(pos:BlockPos,te:TileEntity) = {
    if (!te.isInvalid && te != null)
      TileEntities.put(UnifiedPos(pos,OriginPos,IsRelative = true),te)
  }

  override def addTileEntity(te: TileEntity): Boolean = {
    if (te.isInvalid || te == null) return false
    TileEntities.put(UnifiedPos(te.getPos,OriginPos,IsRelative = true), te)
    true
  }
  override def removeTileEntity(pos:BlockPos): Unit = {
    val te = getTileEntity(pos)
    if (te == null) return
    te.invalidate()
    TileEntities.remove(UnifiedPos(pos,OriginPos,IsRelative = true))
  }


  override def updateEntities(): Unit = {
    if (!isValid)
      return
    TickableTileEntities
      .foreach(pair => {
        def uPos = pair._1
        def te = pair._2
        te.setWorldObj(this) // Just in case they forget :)
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

  override def spawnEntityInWorld(entity:Entity): Boolean = {
    val worldPos = UnifiedVec.convertToWorld(entity.getPositionVector, Ship.getPositionVector)

    entity.setPosition(worldPos.xCoord, worldPos.yCoord, worldPos.zCoord)
    entity.setWorld(OriginWorld)

    OriginWorld.spawnEntityInWorld(entity)
  }
  override def setBlockState(pos: BlockPos, newState: IBlockState, flags: Int): Boolean = {
    if (isRemote) return false

    if (this.isValid && applyBlockChange(pos, newState, flags)) {
      if (!isRemote) {
        if ((flags & 2) != 0) {
          ChangedBlocks.add(new UnifiedPos(pos, Ship.getPosition, true))
          pushBlockChangesToClient()
        }
        if ((flags & 1) != 0) {
          this.notifyNeighborsOfStateChange(pos, newState.getBlock)
          if (newState.getBlock.hasComparatorInputOverride)
            this.updateComparatorOutputLevel(pos, newState.getBlock)
        }


      }
        return true
    }
    false
  }

  // Assumes coordinates are relative to the ship
  override def getClosestPlayer(x: Double, y: Double, z: Double, distance: Double): EntityPlayer = {
    val worldVec = UnifiedVec.convertToWorld(x, y, z, OriginVec())

    val players = OriginWorld.playerEntities.filter(p => !p.isSpectator && p.getDistanceSq(worldVec.xCoord, worldVec.yCoord, worldVec.zCoord) < distance * distance)
    var playerEntity: Option[EntityPlayer] = None

    if (players.nonEmpty)
      playerEntity = Some(players.minBy(_.getDistanceSq(worldVec.xCoord, worldVec.yCoord, worldVec.zCoord)))

    if (playerEntity.isDefined)
      PlayerRelative(playerEntity.get, this)
    else
      null
  }

  def applyBlockChange(pos: BlockPos, newState: IBlockState, flags: Int): Boolean = {
    if (pos == new BlockPos(0,0,0)) return false

    val storage: Option[BlockStorage] = BlockStore.getBlock(pos)

    BlockStore.setBlock(pos, newState)

    if (!isRemote && !isAirBlock(pos) && storage.isEmpty) {
      BlockStore.getBlock(pos).get.BlockState.getBlock.onBlockAdded(this,pos,newState)
    }


    if (storage.isEmpty || newState.getBlock.isInstanceOf[BlockAir])
      Ship.generateBoundingBox()



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

    val relativeStart = UnifiedVec.convertToRelative(start,Ship.getPositionVector)
    val relativeEnd = UnifiedVec.convertToRelative(end,Ship.getPositionVector)

    // The eye position and ray, rotated by the ship block's center, to the opposite of the ships current rotation
    val rotatedStart = VectorUtils.rotatePointFromShip(relativeStart,Ship)
    val rotatedEnd = VectorUtils.rotatePointFromShip(relativeEnd,Ship)

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
    val buffer = new PacketBuffer(Unpooled.buffer())

    buffer.writeInt(TileEntities.size)
    TileEntities.foreach(pair => {
      def uPos = pair._1
      def te = pair._2
      val nbt = new NBTTagCompound()
      te.writeToNBT( nbt )
      buffer.writeNBTTagCompoundToBuffer(nbt)
    })

    val tileEntData = buffer.array()

    (blockData,tileEntData)
  }

  def setWorldData(blockData:Array[Byte],tileEntData:Array[Byte]) = {
    // Block Data
    BlockStore.writeByteData(blockData)

    val buffer = new PacketBuffer(Unpooled.copiedBuffer(tileEntData))

    val teSize = buffer.readInt()
    val tileentities = new Array[TileEntity](teSize)
    for (i <- 0 until teSize)
      tileentities(i) = TileEntity.createAndLoadEntity(buffer.readNBTTagCompoundFromBuffer())

    // Map tile entity positions to UnifiedPositions and then zip it with the tile entities array
    TileEntities = mMap(tileentities.map(te => UnifiedPos(te.getPos,OriginPos,IsRelative = true)).zip(tileentities).toSeq:_*)
  }

  def isValid = BlockStore.nonEmpty

  def needsRenderUpdate() = doRenderUpdate

  def onRenderUpdate() = {doRenderUpdate = false}

  override def playSoundEffect(x:Double,y:Double,z:Double,soundName:String,volume:Float,pitch:Float) = {
    val newVec = UnifiedVec.convertToWorld(new Vec3(x,y,z),ship.getPositionVector)

    OriginWorld.playSoundEffect(newVec.xCoord,newVec.yCoord,newVec.zCoord,soundName,volume,pitch)
  }

  override def playAuxSFXAtEntity(player: EntityPlayer, sfxType: Int, pos: BlockPos, par4:Int) = {
    val newPos = UnifiedPos.convertToWorld(pos,ship.getPosition)
    OriginWorld.playAuxSFXAtEntity(player,sfxType,pos,par4)
  }

  override def spawnParticle(particleType:EnumParticleTypes,x:Double,y:Double,z:Double,xOffset:Double,yOffset:Double,zOffset:Double,par8:Int*) = {
    val rotatedPos = UnifiedVec.convertToWorld(VectorUtils.rotatePointToShip(new Vec3(x,y,z),Ship),Ship.getPositionVector)
    OriginWorld.spawnParticle(particleType,rotatedPos.xCoord,rotatedPos.yCoord,rotatedPos.zCoord,xOffset+Ship.motionX,yOffset+Ship.motionY,zOffset+Ship.motionZ,0)
  }

  // Ripped from doVoidFogParticles in World class
  @SideOnly(Side.CLIENT)
  def doRandomDisplayTick(posX: Int, posY: Int, posZ: Int) = {
    val i: Int = 16
    val random: Random = new Random
    val itemstack: ItemStack = Minecraft.getMinecraft.thePlayer.getHeldItem
    val flag: Boolean = Minecraft.getMinecraft.playerController.getCurrentGameType == WorldSettings.GameType.CREATIVE && itemstack != null && Block.getBlockFromItem(itemstack.getItem) == Blocks.barrier
    val blockpos$mutableblockpos: BlockPos.MutableBlockPos = new BlockPos.MutableBlockPos

    var j: Int = 0
    while (j < 1000) {
        val k: Int = posX + this.rand.nextInt(i) - this.rand.nextInt(i)
        val l: Int = posY + this.rand.nextInt(i) - this.rand.nextInt(i)
        val i1: Int = posZ + this.rand.nextInt(i) - this.rand.nextInt(i)
        blockpos$mutableblockpos.set(k, l, i1)
        val iblockstate: IBlockState = this.getBlockState(blockpos$mutableblockpos)
        iblockstate.getBlock.randomDisplayTick(this, blockpos$mutableblockpos, iblockstate, random)
      if (flag && iblockstate.getBlock == Blocks.barrier)
          this.spawnParticle(EnumParticleTypes.BARRIER, (k.toFloat + 0.5F).toDouble, (l.toFloat + 0.5F).toDouble, (i1.toFloat + 0.5F).toDouble, 0.0D, 0.0D, 0.0D, 0)

      j += 1
    }
  }
  override def isSideSolid(pos:BlockPos,side:EnumFacing,default:Boolean) = getBlockState(pos).getBlock.isSideSolid(this, pos, side)

  override def getBiomeGenForCoords(pos: BlockPos) = OriginWorld.getBiomeGenForCoords(Ship.getPosition.add(pos))

  override def getBiomeGenForCoordsBody(pos: BlockPos) = OriginWorld.getBiomeGenForCoords(Ship.getPosition.add(pos))


}
