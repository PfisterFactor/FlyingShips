package com.MrPf1ster.FlyingShips


import com.MrPf1ster.FlyingShips.entities.EntityShip
import com.google.common.base.Predicates
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase

/**
  * Created by EJ on 3/9/2016.
  */
class FlyingShipEventHandlers {

  @SubscribeEvent
  def playerJoinedWorld(event: EntityJoinWorldEvent) = {
    if (true) {
      // !event.world.isRemote
      // This updates the shipworlds player list to match it's OriginWorld's
      if (event.entity.isInstanceOf[EntityPlayer]) {

        val ships = event.world.getEntities(classOf[EntityShip], Predicates.alwaysTrue[EntityShip]()).iterator()
        while (ships.hasNext) {
          ships.next().ShipWorld.playerEntities.add(event.entity.asInstanceOf[EntityPlayer])
        }

      }
    }
  }

  def updateEntities(world: World): Unit = {
    val ships = world.getEntities(classOf[EntityShip], Predicates.alwaysTrue[EntityShip]())
    if (ships == null || ships.isEmpty) return

    val iterator = ships.iterator
    while (iterator.hasNext) {
      val next = iterator.next()
      next.ShipWorld.updateEntities
    }
  }

  @SubscribeEvent
  def onServerTick(event: TickEvent.WorldTickEvent): Unit = {
    //Minecraft.getMinecraft.thePlayer.setInvisible(true)
    if (event.phase != Phase.START) return
    updateEntities(event.world)

  }

  @SubscribeEvent
  def onClientTick(event: TickEvent.ClientTickEvent): Unit = {

    if (event.phase != Phase.START) return

    try {
      def world = Minecraft.getMinecraft.getRenderViewEntity.asInstanceOf[EntityPlayer].getEntityWorld

      updateEntities(world)
    }
    catch {
      case e: Exception => {}
    }


  }
}
