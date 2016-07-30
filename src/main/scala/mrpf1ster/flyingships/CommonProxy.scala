package mrpf1ster.flyingships

import mrpf1ster.flyingships.blocks.ShipCreatorBlock
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.util.BlockUtils
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import net.minecraftforge.fml.common.registry.EntityRegistry
import net.minecraftforge.fml.relauncher.Side

/**
  * Created by EJ on 2/21/2016.
  */
object CommonProxy {
  var ShipCreatorBlock: ShipCreatorBlock = null
  // Tracking Range and frequency
  val ShipTracking = (256, 5)

  def preInit(event:FMLPreInitializationEvent) = {
    ShipCreatorBlock = new ShipCreatorBlock()
    MinecraftForge.EVENT_BUS.register(FlyingShips.flyingShipEventHandlers) // Register our events

    if (event.getSide == Side.SERVER)
      FlyingShips.flyingShipPacketHandler.registerServerSide()

    BlockUtils.loadInClasses // So game doesn't hang when implementing the Scala predefined library
  }
  def init(event: FMLInitializationEvent) = {
    EntityRegistry.registerModEntity(classOf[EntityShip], "Ship Entity", 0, FlyingShips, ShipTracking._1, ShipTracking._2, true)

  }
  def postInit(event: FMLPostInitializationEvent) = {

  }
}
