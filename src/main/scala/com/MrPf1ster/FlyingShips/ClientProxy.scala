package com.MrPf1ster.FlyingShips

import com.MrPf1ster.FlyingShips.blocks.ShipCreatorBlock
import com.MrPf1ster.FlyingShips.entities.ShipEntity
import com.MrPf1ster.FlyingShips.render.ShipRender
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}


/**
  * Created by EJ on 2/21/2016.
  */
object ClientProxy {
  var shipCreatorBlock: ShipCreatorBlock = null
  def preInit(event:FMLPreInitializationEvent) = {

    FlyingShips.flyingShipPacketHandler.registerClientSide()
    shipCreatorBlock = new ShipCreatorBlock()
  }
  def init(event: FMLInitializationEvent) = {
    val renderItem = Minecraft.getMinecraft.getRenderItem



    // Blocks
    renderItem.getItemModelMesher.register(Item.getItemFromBlock(shipCreatorBlock), 0, new ModelResourceLocation(FlyingShips.MOD_ID + ":" + shipCreatorBlock.name, "inventory"))

    // Items
    //renderItem.getItemModelMesher().register(shipCreatorBlockItem, 0, new ModelResourceLocation(MOD_ID + ":" + shipCreatorBlockItem.getName(), "inventory"));

    // Entities
    def rm = Minecraft.getMinecraft.getRenderManager
    val shipRender = new ShipRender(rm)
    RenderingRegistry.registerEntityRenderingHandler(classOf[ShipEntity],shipRender)
  }
  def postInit(event: FMLPostInitializationEvent) = {

  }
}
