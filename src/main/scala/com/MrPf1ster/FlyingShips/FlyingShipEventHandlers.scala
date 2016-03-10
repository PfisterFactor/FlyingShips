package com.MrPf1ster.FlyingShips


import com.MrPf1ster.FlyingShips.entities.ShipEntity
import com.google.common.base.Predicates
import net.minecraft.entity.player.EntityPlayer
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
        val ships = event.world.getEntities(classOf[ShipEntity], Predicates.alwaysTrue[ShipEntity]()).iterator()
        while (ships.hasNext) {
          ships.next().ShipWorld.playerEntities.add(event.entity.asInstanceOf[EntityPlayer])
        }

      }
    }
  }

  @SubscribeEvent
  def onTick(event: TickEvent.WorldTickEvent): Unit = {
    if (event.phase != Phase.START) return

    val ships = event.world.getEntities(classOf[ShipEntity], Predicates.alwaysTrue[ShipEntity]())
    if (ships == null || ships.isEmpty) return

    val iterator = ships.iterator
    while (iterator.hasNext) {
      val next = iterator.next()
      next.ShipWorld.updateEntities
    }


  }
}
