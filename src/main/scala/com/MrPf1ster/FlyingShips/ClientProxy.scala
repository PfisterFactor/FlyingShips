package com.MrPf1ster.FlyingShips

import com.MrPf1ster.FlyingShips.render.ShipRender
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraftforge.fml.client.registry.{RenderingRegistry, ClientRegistry}
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.{FMLPostInitializationEvent, FMLPreInitializationEvent, FMLInitializationEvent}
import net.minecraftforge.fml.relauncher.{SideOnly, Side}
import com.MrPf1ster.FlyingShips.entities.ShipEntity


/**
  * Created by EJ on 2/21/2016.
  */
object ClientProxy {

  def preInit(event:FMLPreInitializationEvent) = {


  }
  def init(event: FMLInitializationEvent) = {
    val renderItem = Minecraft.getMinecraft().getRenderItem();



    // Blocks
    renderItem.getItemModelMesher().register(Item.getItemFromBlock(FlyingShips.shipCreatorBlock), 0, new ModelResourceLocation(FlyingShips.MOD_ID + ":" + FlyingShips.shipCreatorBlock.name, "inventory"));

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
