package com.MrPf1ster.FlyingShips

import com.MrPf1ster.FlyingShips.blocks.ShipCreatorBlock
import com.MrPf1ster.FlyingShips.entities.ShipEntity
import com.MrPf1ster.FlyingShips.util.BlockUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraftforge.fml.common.event.{FMLPostInitializationEvent, FMLInitializationEvent, FMLPreInitializationEvent}
import net.minecraftforge.fml.common.registry.EntityRegistry

import scala.collection.immutable.Queue
import scala.collection.mutable

/**
  * Created by EJ on 2/21/2016.
  */
object CommonProxy {
  def preInit(event:FMLPreInitializationEvent) = {
    FlyingShips.shipCreatorBlock = new ShipCreatorBlock()
    BlockUtils.loadInClasses // So game doesn't hang when implementing the Scala predefined library
  }
  def init(event: FMLInitializationEvent) = {
    EntityRegistry.registerModEntity(classOf[ShipEntity],"Ship Entity",0,FlyingShips,256,10,true)
  }
  def postInit(event: FMLPostInitializationEvent) = {

  }
}
