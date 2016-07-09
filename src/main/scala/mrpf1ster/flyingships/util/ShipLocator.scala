package mrpf1ster.flyingships.util

import java.util.UUID

import mrpf1ster.flyingships.entities.EntityShip
import net.minecraft.world.World

import scala.collection.JavaConversions._


/**
  * Created by ej on 7/1/16.
  */
object ShipLocator {
  def getShips(world:World): Set[EntityShip] = {
    if (world == null) return Set()
    val worldEntities = world.getLoadedEntityList
    worldEntities.filter(_.isInstanceOf[EntityShip]).map(_.asInstanceOf[EntityShip]).toSet
  }

  def getShip(world:World, entityID:Int): Option[EntityShip] = {
    getShips(world).find(_.getEntityId == entityID)
  }

  def getShip(world:World, entityUUID:UUID): Option[EntityShip] = {
    getShips(world).find(_.getPersistentID == entityUUID)
  }
}
