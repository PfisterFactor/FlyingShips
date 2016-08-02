package mrpf1ster.flyingships

/**
  * Created by MrPf1ster
  */

import mrpf1ster.flyingships.command.DeleteAllShipsCommand
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.network.FlyingShipsPacketHandler
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event._
import org.apache.logging.log4j.LogManager


@Mod(modid = FlyingShips.MOD_ID, name = "Flying Ships", version = FlyingShips.VERSION, modLanguage = "scala")
object FlyingShips {

  final val MOD_ID = "flyingships"
  final val VERSION = "0.01"
  val logger = LogManager.getLogger("Flying Ships")

  val flyingShipEventHandlers = new FlyingShipEventHandlers
  val flyingShipPacketHandler = new FlyingShipsPacketHandler




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
  }

  @EventHandler
  def onServerStop(event: FMLServerStoppedEvent): Unit = {
    EntityShip.resetIDs()
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
