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
      EntityShipTracker.Ships.collect({ case pair: (Int, ShipTrackerEntry) if pair._2.ShipEntity.worldObj.provider.getDimensionId == world.provider.getDimensionId => pair._2.ShipEntity }).toSet
    else
      EntityShipTracker.ClientSideShips.collect({ case pair: (Int, EntityShip) if pair._2.worldObj.provider.getDimensionId == world.provider.getDimensionId => pair._2 }).toSet
  }

  def getClientShips: Set[EntityShip] = EntityShipTracker.ClientSideShips.values.toSet

  def getServerShips: Set[EntityShip] = EntityShipTracker.Ships.values.map(_.ShipEntity).toSet

  def getShips(world: World, aabb: AxisAlignedBB): Set[EntityShip] = {
    if (world == null) return Set()
    getShips(world).filter(_.getEntityBoundingBox.intersectsWith(aabb))
  }

  def getServerShip(shipID: Int): Option[EntityShip] = getServerShips.find(_.ShipID == shipID)

  def getClientShip(shipID: Int): Option[EntityShip] = getClientShips.find(_.ShipID == shipID)

  def getShip(world: World, shipID: Int): Option[EntityShip] = getShips(world).find(_.ShipID == shipID)

  def getShip(world: World, shipWorldUUID: UUID): Option[EntityShip] = getShips(world).find(_.Shipworld.UUID == shipWorldUUID)

  def getShip(movingObjectPosition: MovingObjectPosition): Option[EntityShip] = {
    if (movingObjectPosition == null || movingObjectPosition.typeOfHit != MovingObjectType.ENTITY || !movingObjectPosition.entityHit.isInstanceOf[EntityShip])
      Some(movingObjectPosition.entityHit.asInstanceOf[EntityShip])
    else
      None
  }
}
