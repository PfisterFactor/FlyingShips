package mrpf1ster.flyingships.entities

import mrpf1ster.flyingships.CommonProxy
import mrpf1ster.flyingships.network.DeleteShipMessage
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.fml.common.network.simpleimpl.IMessage

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
  * Created by EJ on 7/29/2016.
  */
object EntityShipTracker {
  private val ships: mutable.Map[Int, ShipTrackerEntry] = mutable.Map()


  def Ships = ships

  private val TrackingRange = CommonProxy.ShipTracking._1
  private val TrackingFrequency = CommonProxy.ShipTracking._2
  val ClientSideShips: mutable.Map[Int, EntityShip] = mutable.Map()

  def addShipClientSide(entityShip: EntityShip): Unit = {
    ClientSideShips.put(entityShip.ShipID, entityShip)
  }

  def removeShipClientSide(entityShip: EntityShip): Unit = {
    ClientSideShips.remove(entityShip.ShipID)
  }

  private def addShipToTracker(entityShip: EntityShip): Unit = {
    if (entityShip.Shipworld.OriginWorld.isRemote) return
    ships.put(entityShip.ShipID, new ShipTrackerEntry(entityShip, TrackingRange, TrackingFrequency))

  }

  private def removeShipFromTracker(entityShip: EntityShip): Unit = {
    if (entityShip.Shipworld.OriginWorld.isRemote) return
    val shipTracker = ships.get(entityShip.ShipID)
    if (shipTracker.isDefined) {
      val message = new DeleteShipMessage(entityShip.ShipID)
      shipTracker.get.sendMessageToTrackedPlayers(message)
    }
    ships.remove(entityShip.ShipID)
  }

  def trackShip(entityShip: EntityShip) = {
    if (entityShip != null && entityShip.Shipworld != null)
      addShipToTracker(entityShip)
  }

  def untrackShip(entityShip: EntityShip) = {
    if (entityShip != null)
      removeShipFromTracker(entityShip)
  }

  def updateTrackedShips() = {
    ships.values.foreach(shipTracker => {
      shipTracker.updatePlayerList(shipTracker.ShipEntity.worldObj.playerEntities.toList)
    })
  }

  def sendToAllTrackingShip(shipID: Int, message: IMessage): Boolean = {
    val ship = ships.get(shipID)
    if (ship.isDefined) {
      ship.get.sendMessageToTrackedPlayers(message)
      true
    }
    else
      false
  }

  def onPlayerUpdate(playerMP: EntityPlayerMP) = {
    ships.values.foreach(shipTracker => {
      if (shipTracker.withinTrackingDistance(playerMP))
        shipTracker.updatePlayerEntity(playerMP)
    })
  }
}
