package mrpf1ster.flyingships.network

import javax.vecmath.Quat4f

import com.unascribed.lambdanetwork.{DataType, PendingPacket}
import io.netty.buffer.Unpooled
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.world.ShipWorld
import net.minecraft.block.{Block, BlockEventData}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.server.{S21PacketChunkData, S35PacketUpdateTileEntity}
import net.minecraft.util._
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by EJPfi on 1/13/2017.
  */
object PacketSender {

  @SideOnly(Side.CLIENT)
  def sendBlockActionPacket(shipID:Int, event: BlockEventData):PendingPacket = {
    FlyingShips.network.send().packet("BlockAction")
      .`with`("ShipID",shipID)
      .`with`("BlockPosition",event.getPosition.toLong)
      .`with`("BlockID", Block.getIdFromBlock(event.getBlock))
      .`with`("Data1", event.getEventID)
      .`with`("Data2",event.getEventParameter)
  }

  @SideOnly(Side.CLIENT)
  def sendBlockDiggingPacket(shipID: Int, status: C07PacketPlayerDigging.Action, pos: BlockPos, side: EnumFacing): PendingPacket = {
    FlyingShips.network.send().packet("BlockDigging")
      .`with` ("ShipID", shipID)
      .`with`("Status",status.ordinal())
      .`with`("BlockPosition",pos.toLong)
      .`with`("Side",side.ordinal())
  }

  def sendBlockChangedPacket(shipWorld:ShipWorld, blockPos:BlockPos): PendingPacket = {
    val te = shipWorld.getTileEntity(blockPos)
    val nbt = new NBTTagCompound()
    if (te != null)
      te.writeToNBT(nbt)

    FlyingShips.network.send().packet("BlockChanged")
      .`with` ("ShipID", shipWorld.Ship.ShipID)
      .`with` ("BlockPosition", blockPos.toLong)
      .`with` ("Blockstate", Block.getStateId(shipWorld.getBlockState(blockPos)))
      .`with`("HasTileEntity", !nbt.hasNoTags)
      .`with` ("TileEntity", nbt)
  }


  def sendBlockPlacedPacket(shipID:Int, blockPos:BlockPos,side:EnumFacing,heldItem:ItemStack,hitVec:Vec3): PendingPacket = {
    val bytebuf = Unpooled.buffer()
    ByteBufUtils.writeItemStack(bytebuf,heldItem)
    val data = new Array[Byte](bytebuf.readableBytes())
    bytebuf.readBytes(data)

    val sideNum = if (side != null) side.getIndex else 255

    FlyingShips.network.send().packet("BlockPlaced")
      .`with` ("ShipID",shipID)
      .`with`("HitVecX",hitVec.xCoord)
      .`with`("HitVecY",hitVec.yCoord)
      .`with`("HitVecZ",hitVec.zCoord)
      .`with`("BlockPosition",blockPos)
      .`with`("Side",sideNum)
      .`with`("HeldItem",data)
  }

  def sendChunkDataPacket(shipID: Int, chunk: Chunk, par2: Boolean, par3: Int): PendingPacket = {
    val chunkData = S21PacketChunkData.func_179756_a(chunk, par2, true, par3)

    FlyingShips.network.send().packet("ChunkData")
      .`with`("ShipID",shipID)
      .`with`("Par2",par2)
      .`with`("ChunkData",chunkData.data)
      .`with`("ChunkX", chunk.xPosition)
      .`with`("ChunkZ",chunk.zPosition)
  }

  def sendDeleteShipPacket(shipID:Int):PendingPacket = {
    FlyingShips.network.send().packet("DeleteShip")
      .`with`("ShipID",shipID)
  }

  def sendMultipleBlocksChangedPacket(shipID: Int, size:Int, crammedPositions: Array[Short], chunk: Chunk): PendingPacket = {
    val blockData = (0 until size).map(i => new PacketHandlers.MultipleBlocksChangedHandler.BlockUpdateData(crammedPositions(i),chunk))
    val byteBuf = Unpooled.buffer()
    blockData.foreach(data => {byteBuf.writeShort(data.getChunkPosCrammed); byteBuf.writeInt(Block.getStateId(data.blockState))})

    val data = new Array[Byte](byteBuf.readableBytes())
    byteBuf.readBytes(data)
    FlyingShips.network.send().packet("MultipleBlocksChanged")
      .`with`("ShipID",shipID)
      .`with`("ChunkX",chunk.xPosition)
      .`with`("ChunkZ",chunk.zPosition)
      .`with`("ChangedBlocks",data)

  }

  def sendShipMovementPacket(shipID:Int, pos: Option[Vec3], rot: Option[Quat4f], isRelative: Boolean): PendingPacket = {
    val byteBuf = Unpooled.buffer()

    def encodePos(x:Double):Int = MathHelper.floor_double(x * 32.0D)
    def encodeRot(x:Float):Int = MathHelper.floor_double(x * 1000000.0D)

    if (pos.isDefined) {
      byteBuf.writeInt(encodePos(pos.get.xCoord))
      byteBuf.writeInt(encodePos(pos.get.yCoord))
      byteBuf.writeInt(encodePos(pos.get.zCoord))
    }
    if (rot.isDefined) {
      byteBuf.writeInt(encodeRot(rot.get.getX))
      byteBuf.writeInt(encodeRot(rot.get.getY))
      byteBuf.writeInt(encodeRot(rot.get.getZ))
      byteBuf.writeInt(encodeRot(rot.get.getW))
    }

    if (isRelative) byteBuf.writeBoolean(isRelative)

    val data = new Array[Byte](byteBuf.readableBytes())
    byteBuf.readBytes(data)

    FlyingShips.network.send().packet("ShipMovement")
      .`with`("ShipID",shipID)
      .`with`("MovementData",data)
  }

  def sendUpdateTileEntityPacket(shipID:Int, blockPos: BlockPos, metadata: Int, nbt: NBTTagCompound): PendingPacket = {
    FlyingShips.network.send().packet("UpdateTileEntity")
      .`with`("ShipID",shipID)
      .`with`("BlockPosition",blockPos)
      .`with`("Metadata",metadata)
      .`with`("NBT",nbt)
  }

  def sendUpdateTileEntityPacket(shipID:Int, packet:S35PacketUpdateTileEntity): PendingPacket = sendUpdateTileEntityPacket(shipID,packet.getPos,packet.getTileEntityType,packet.getNbtCompound)

  def sendShipVelocityPacket(shipID:Int,motionX:Double,motionY:Double,motionZ:Double) = {
    def encodeDouble(x: Double): Int = (x * 8000).toInt
    FlyingShips.network.send().packet("ShipVelocity")
      .`with`("ShipID",shipID)
      .`with`("MotionX",encodeDouble(motionX))
      .`with`("MotionY",encodeDouble(motionY))
      .`with`("MotionZ",encodeDouble(motionZ))
  }
}
