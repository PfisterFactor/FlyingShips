package com.MrPf1ster.FlyingShips


import com.MrPf1ster.FlyingShips.util.ShipLocator
import net.minecraft.client.Minecraft
import net.minecraft.world.World
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase

/**
  * Created by EJ on 3/9/2016.
  */
class FlyingShipEventHandlers {

  def updateEntities(world: World): Unit = {
    val ships = ShipLocator.getShips(world)
    if (ships.isEmpty) return

    ships.foreach(ship => ship.ShipWorld.updateEntities())
  }

  @SubscribeEvent
  def onServerTick(event: TickEvent.WorldTickEvent): Unit = {
    //Minecraft.getMinecraft.thePlayer.setInvisible(true)
    if (event.phase != Phase.START) return
    updateEntities(event.world)

  }

  @SubscribeEvent
  def onClientTick(event: TickEvent.ClientTickEvent): Unit = {
    if (event.phase != TickEvent.Phase.END) return
    val ships = ShipLocator.getShips(Minecraft.getMinecraft.theWorld)
    if (ships.isEmpty) return



    ships.foreach(ship => {
      ship.InteractionHandler.ClickSimulator.sendClickBlockToController(Minecraft.getMinecraft.thePlayer)
    })
  }
}
