package com.MrPf1ster.FlyingShips

/**
  * Created by MrPf1ster
  */

import com.MrPf1ster.FlyingShips.network.FlyingShipsPacketHandler
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import net.minecraftforge.fml.common.{Mod, ModContainer}


@Mod(modid = FlyingShips.MOD_ID, name = "Flying Ships", version = FlyingShips.VERSION, modLanguage = "scala")
object FlyingShips extends ModContainer{

  implicit class BlockPosExtension(pos: BlockPos) {
    def -(pos2: BlockPos) = new BlockPos(pos.getX - pos2.getX, pos.getY - pos2.getY, pos.getZ - pos2.getZ)
  }


  final val MOD_ID = "flyingships"
  final val VERSION = "0.01"
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
    System.out.println("Flying Ships are taking off!")
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
}
