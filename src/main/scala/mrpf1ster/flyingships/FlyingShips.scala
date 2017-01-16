package mrpf1ster.flyingships

/**
  * Created by MrPf1ster
  */

import com.unascribed.lambdanetwork.{DataType, LambdaNetwork}
import mrpf1ster.flyingships.command.{DeleteAllShipsCommand, TeleportShipCommand}
import mrpf1ster.flyingships.network.{FlyingShipsPacketHandler, PacketHandlers}
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event._
import net.minecraftforge.fml.relauncher.Side
import org.apache.logging.log4j.LogManager


@Mod(modid = FlyingShips.MOD_ID, name = "Flying Ships", version = FlyingShips.VERSION, modLanguage = "scala")
object FlyingShips {

  final val MOD_ID = "flyingships"
  final val VERSION = "0.01"
  val logger = LogManager.getLogger("Flying Ships")

  val flyingShipEventHandlers = new FlyingShipEventHandlers
  val flyingShipPacketHandler = new FlyingShipsPacketHandler

  // Network packet definition using unascribed's wonderful LambdaNetwork library
  // https://github.com/elytra/LambdaNetwork
  val network = LambdaNetwork.builder().channel("flyingships1")
    .packet("BlockAction")
      .boundTo(Side.CLIENT)
      .`with`(DataType.INT,"ShipID")
      .`with`(DataType.LONG,"BlockPosition")
      .`with`(DataType.INT,"Data1")
      .`with`(DataType.INT,"Data2")
      .`with`(DataType.INT,"BlockID")
      .handledOnMainThreadBy(PacketHandlers.BlockActionHandler)
    .packet("BlockChanged")
      .boundTo(Side.CLIENT)
      .`with`(DataType.INT,"ShipID")
      .`with`(DataType.LONG,"BlockPosition")
      .`with`(DataType.INT,"Blockstate")
      .`with`(DataType.BOOLEAN,"HasTileEntity")
     .`with`(DataType.NBT_COMPOUND,"TileEntity")
      .handledOnMainThreadBy(PacketHandlers.BlockChangedHandler)
    .packet("BlockDigging")
      .boundTo(Side.SERVER)
      .`with`(DataType.INT,"ShipID")
      .`with`(DataType.INT,"Status")
      .`with`(DataType.LONG,"BlockPosition")
      .`with`(DataType.INT,"Side")
      .handledOnMainThreadBy(PacketHandlers.BlockDiggingHandler)
    .packet("BlockPlaced")
      .boundTo(Side.SERVER)
      .`with` (DataType.INT,"ShipID")
      .`with`(DataType.DOUBLE,"HitVecX")
      .`with`(DataType.DOUBLE,"HitVecY")
      .`with`(DataType.DOUBLE,"HitVecZ")
      .`with`(DataType.LONG,"BlockPosition")
      .`with`(DataType.INT,"Side")
      .`with`(DataType.BYTE_ARRAY,"HeldItem")
      .handledOnMainThreadBy(PacketHandlers.BlockPlacedHandler)
    .packet("ChunkData")
      .boundTo(Side.CLIENT)
      .`with`(DataType.INT,"ShipID")
      .`with`(DataType.BOOLEAN,"Par2")
      .`with`(DataType.INT,"ChunkX")
      .`with`(DataType.INT,"ChunkY")
      .`with`(DataType.BYTE_ARRAY,"ChunkData")
      .handledOnMainThreadBy(PacketHandlers.ChunkDataHandler)
    .packet("DeleteShip")
      .boundTo(Side.CLIENT)
      .`with`(DataType.INT,"ShipID")
      .handledOnMainThreadBy(PacketHandlers.DeleteShipHandler)
    .packet("MultipleBlocksChanged")
      .boundTo(Side.CLIENT)
      .`with`(DataType.INT,"ShipID")
      .`with`(DataType.INT,"ChunkX")
      .`with`(DataType.INT,"ChunkZ")
      .`with`(DataType.BYTE_ARRAY,"ChangedBlocks")
      .handledOnMainThreadBy(PacketHandlers.MultipleBlocksChangedHandler)
    .packet("ShipMovement")
      .boundTo(Side.CLIENT)
      .`with`(DataType.INT,"ShipID")
      .`with`(DataType.BYTE_ARRAY,"MovementData")
      .handledOnMainThreadBy(PacketHandlers.ShipMovementHandler)
      .packet("ShipVelocity")
      .boundTo(Side.CLIENT)
      .`with`(DataType.INT,"ShipID")
      .`with`(DataType.INT,"MotionX")
      .`with`(DataType.INT,"MotionY")
      .`with`(DataType.INT,"MotionZ")
      .handledOnMainThreadBy(PacketHandlers.ShipVelocityHandler)
    .packet("UpdateTileEntity")
      .boundTo(Side.CLIENT)
      .`with`(DataType.INT,"ShipID")
      .`with`(DataType.LONG,"BlockPosition")
      .`with`(DataType.INT,"Metadata")
      .`with`(DataType.NBT_COMPOUND,"NBT")
      .handledOnMainThreadBy(PacketHandlers.UpdateTileEntityHandler)
  .build()




  @EventHandler
  def preInit(event: FMLPreInitializationEvent) = {
    CommonProxy.preInit(event)
    if (event.getSide.isClient) {
      ClientProxy.preInit(event)
    }

  }

  @EventHandler
  def init(event: FMLInitializationEvent) = {
    logger.info("Flying Ships are taking off!")
    CommonProxy.init(event)
    if (event.getSide.isClient) {
      ClientProxy.init(event)
    }





  }

  @EventHandler
  def postInit(event: FMLPostInitializationEvent) = {
    CommonProxy.postInit(event)
    if (event.getSide.isClient) {
      ClientProxy.postInit(event)
    }
  }

  @EventHandler
  def registerCommands(event: FMLServerStartingEvent) = {
    event.registerServerCommand(new DeleteAllShipsCommand())
    event.registerServerCommand(new TeleportShipCommand())
  }

  @EventHandler
  def onServerStop(event: FMLServerStoppedEvent): Unit = {
  }

  // Debug usage
  def time[R](block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block // call-by-name
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
    result
  }
}
