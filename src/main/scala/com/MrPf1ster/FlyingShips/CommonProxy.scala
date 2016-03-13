package com.MrPf1ster.FlyingShips

import com.MrPf1ster.FlyingShips.entities.ShipEntity
import com.MrPf1ster.FlyingShips.util.BlockUtils
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import net.minecraftforge.fml.common.registry.EntityRegistry
import net.minecraftforge.fml.relauncher.Side

/**
  * Created by EJ on 2/21/2016.
  */
object CommonProxy {
  def preInit(event:FMLPreInitializationEvent) = {
    MinecraftForge.EVENT_BUS.register(FlyingShips.flyingShipEventHandlers) // Register our events

    if (event.getSide == Side.SERVER)
      FlyingShips.flyingShipPacketHandler.registerServerSide()

    BlockUtils.loadInClasses // So game doesn't hang when implementing the Scala predefined library
  }
  def init(event: FMLInitializationEvent) = {
    EntityRegistry.registerModEntity(classOf[ShipEntity],"Ship Entity",0,FlyingShips,256,10,true)
  }
  def postInit(event: FMLPostInitializationEvent) = {

  }
}
