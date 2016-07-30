package mrpf1ster.flyingships.util

import java.util.UUID

import mrpf1ster.flyingships.entities.{EntityShip, EntityShipTracker, ShipTrackerEntry}
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.{AxisAlignedBB, MovingObjectPosition}
import net.minecraft.world.World


/**
  * Created by ej on 7/1/16.
  */
object ShipLocator {
  def getShips(world:World): Set[EntityShip] = {
    if (world == null) return Set()
    if (!world.isRemote)
      EntityShipTracker.Ships.collect({ case pair: (Int, ShipTrackerEntry) if pair._2.ShipEntity.worldObj == world => pair._2.ShipEntity }).toSet
    else
      EntityShipTracker.ClientSideShips.collect({ case pair: (Int, EntityShip) if pair._2.worldObj == world => pair._2 }).toSet
  }

  // Guesses if on client and returns a ship set
  def getShips: Set[EntityShip] = {
    if (EntityShipTracker.ClientSideShips.nonEmpty)
      EntityShipTracker.ClientSideShips.values.toSet
    else
      EntityShipTracker.Ships.values.map(_.ShipEntity).toSet
  }

  def getShips(world: World, aabb: AxisAlignedBB): Set[EntityShip] = {
    if (world == null) return Set()
    getShips(world).filter(_.getEntityBoundingBox.intersectsWith(aabb))
  }

  def getShip(shipID: Int): Option[EntityShip] = getShips.find(_.ShipID == shipID)

  def getShip(world: World, shipID: Int): Option[EntityShip] = getShips(world).find(_.getEntityId == shipID)

  def getShip(world: World, shipWorldUUID: UUID): Option[EntityShip] = getShips(world).find(_.Shipworld.UUID == shipWorldUUID)

  def getShip(movingObjectPosition: MovingObjectPosition): Option[EntityShip] = {
    if (movingObjectPosition == null || movingObjectPosition.typeOfHit != MovingObjectType.ENTITY || !movingObjectPosition.entityHit.isInstanceOf[EntityShip])
      Some(movingObjectPosition.entityHit.asInstanceOf[EntityShip])
    else
      None
  }
}
