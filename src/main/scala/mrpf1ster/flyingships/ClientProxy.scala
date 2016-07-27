package mrpf1ster.flyingships

import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.render.ShipRender
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}


/**
  * Created by EJ on 2/21/2016.
  */
object ClientProxy {

  def preInit(event:FMLPreInitializationEvent) = {
    FlyingShips.flyingShipPacketHandler.registerClientSide()

  }
  def init(event: FMLInitializationEvent) = {

    def modelMesher = Minecraft.getMinecraft.getRenderItem.getItemModelMesher
    // Blocks
    modelMesher.register(Item.getItemFromBlock(CommonProxy.shipCreatorBlock), 0, new ModelResourceLocation(FlyingShips.MOD_ID + ":" + CommonProxy.shipCreatorBlock.name, "inventory"))




    // Entities
    def rm = Minecraft.getMinecraft.getRenderManager
    val shipRender = new ShipRender(rm)
    //noinspection ScalaDeprecation
    RenderingRegistry.registerEntityRenderingHandler(classOf[EntityShip],shipRender)
  }
  def postInit(event: FMLPostInitializationEvent) = {

  }
}
