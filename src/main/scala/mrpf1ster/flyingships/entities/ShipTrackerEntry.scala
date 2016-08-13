package mrpf1ster.flyingships.entities

import javax.vecmath.Quat4f

import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.network._
import mrpf1ster.flyingships.world.ShipWorldServer
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.util.MathHelper
import net.minecraftforge.fml.common.network.simpleimpl.IMessage

import scala.collection.mutable

/**
  * Created by EJ on 7/29/2016.
  */
case class ShipTrackerEntry(ShipEntity: EntityShip, TrackingRange: Int, TrackingFrequency: Int) {

  var encodedPosX: Int = MathHelper.floor_double(ShipEntity.posX * 32.0D)
  var encodedPosY: Int = MathHelper.floor_double(ShipEntity.posY * 32.0D)
  var encodedPosZ: Int = MathHelper.floor_double(ShipEntity.posZ * 32.0D)
  var rotation: Quat4f = ShipEntity.getRotation
  var lastTrackedEntityMotionX: Double = 0
  var lastTrackedEntityMotionY: Double = 0
  var lastTrackedEntityMotionZ: Double = 0
  var updateCounter: Int = 0
  private var lastTrackedEntityPosX: Double = 0
  private var lastTrackedEntityPosY: Double = 0
  private var lastTrackedEntityPosZ: Double = 0
  private var lastTrackedRotation: Quat4f = rotation
  private var firstUpdateDone: Boolean = false
  private var ticksSinceLastForcedTeleport: Int = 0

  var PlayerEntitiesUpdated: Boolean = false
  var TrackingPlayers: mutable.Set[EntityPlayerMP] = mutable.Set()

  override def equals(other: Any) = other.isInstanceOf[ShipTrackerEntry] && other.asInstanceOf[ShipTrackerEntry].ShipEntity.ShipID == ShipEntity.ShipID

  override def hashCode(): Int = ShipEntity.ShipID

  def sendMessageToTrackedPlayers(message: IMessage) = TrackingPlayers.foreach(p => FlyingShips.flyingShipPacketHandler.INSTANCE.sendTo(message, p))

  def updatePlayerList(players: List[EntityPlayer]): Unit = {
    PlayerEntitiesUpdated = false

    if (!firstUpdateDone || ShipEntity.getDistanceSq(this.lastTrackedEntityPosX, this.lastTrackedEntityPosY, this.lastTrackedEntityPosZ) > 16.0D) {
      lastTrackedEntityPosX = ShipEntity.posX
      lastTrackedEntityPosY = ShipEntity.posY
      lastTrackedEntityPosZ = ShipEntity.posZ
      firstUpdateDone = true
      PlayerEntitiesUpdated = true
      updatePlayerEntities(players.map(_.asInstanceOf[EntityPlayerMP]).toSet)
    }

    if (updateCounter % TrackingFrequency == 0 || ShipEntity.getDataWatcher.hasObjectChanged) {

      ticksSinceLastForcedTeleport += 1
      // Todo: Implement rotation in here rather than in datawatcher
      val encodedShipPosX: Int = MathHelper.floor_double(ShipEntity.posX * 32.0D)
      val encodedShipPosY: Int = MathHelper.floor_double(ShipEntity.posY * 32.0D)
      val encodedShipPosZ: Int = MathHelper.floor_double(ShipEntity.posZ * 32.0D)
      val deltaPosX: Int = encodedShipPosX - this.encodedPosX
      val deltaPosY: Int = encodedShipPosY - this.encodedPosY
      val deltaPosZ: Int = encodedShipPosZ - this.encodedPosZ
      var message: IMessage = null
      val sendMoveMsg: Boolean = Math.abs(deltaPosX) >= 4 || Math.abs(deltaPosY) >= 4 || Math.abs(deltaPosZ) >= 4 || this.updateCounter % 60 == 0
      val sendRotMsg: Boolean = !rotation.epsilonEquals(ShipEntity.getRotation, 0.01f)

      if (updateCounter > 0) {
        if (deltaPosX >= -128 && deltaPosX < 128 && deltaPosY >= -128 && deltaPosY < 128 && deltaPosZ >= -128 && deltaPosZ < 128 && this.ticksSinceLastForcedTeleport <= 400) {
          if (!sendMoveMsg || !sendRotMsg) {
            // If both are true don't execute
            if (sendMoveMsg)
              message = new ShipRelMoveMessage(ShipEntity.ShipID, deltaPosX.toByte, deltaPosY.toByte, deltaPosZ.toByte)
            else if (sendRotMsg)
              message = new ShipRotMessage(ShipEntity)
          }
          else
            message = new ShipMoveRotMessage(ShipEntity.ShipID, deltaPosX, deltaPosY, deltaPosZ, ShipEntity.getRotation, false)
        }
        else {
          ticksSinceLastForcedTeleport = 0
          message = new ShipMoveRotMessage(ShipEntity.ShipID, encodedShipPosX, encodedShipPosY, encodedShipPosZ, ShipEntity.getRotation, true)
        }
      }

      val d0: Double = ShipEntity.motionX - this.lastTrackedEntityMotionX
      val d1: Double = ShipEntity.motionY - this.lastTrackedEntityMotionY
      val d2: Double = ShipEntity.motionZ - this.lastTrackedEntityMotionZ
      val d3: Double = 0.02D
      val d4: Double = d0 * d0 + d1 * d1 + d2 * d2

      if (d4 > d3 * d3 || d4 > 0.0D && ShipEntity.motionX == 0.0D && ShipEntity.motionY == 0.0D && ShipEntity.motionZ == 0.0D) {
        this.lastTrackedEntityMotionX = ShipEntity.motionX
        this.lastTrackedEntityMotionY = ShipEntity.motionY
        this.lastTrackedEntityMotionZ = ShipEntity.motionZ
        val velocityMessage = new ShipVelocityMessage(ShipEntity.ShipID, lastTrackedEntityMotionX, lastTrackedEntityMotionY, lastTrackedEntityMotionZ)
        sendMessageToTrackedPlayers(velocityMessage)
      }

      if (message != null)
        sendMessageToTrackedPlayers(message)

      //sendMetadataToAllAssociatedPlayers()

      if (sendMoveMsg) {
        this.encodedPosX = encodedShipPosX
        this.encodedPosY = encodedShipPosY
        this.encodedPosZ = encodedShipPosZ
      }
      if (sendRotMsg) {
        rotation = ShipEntity.getRotation
      }
    }
    updateCounter += 1

    if (ShipEntity.velocityChanged) {
      val velocityMessage = new ShipVelocityMessage(ShipEntity.ShipID, ShipEntity.motionX, ShipEntity.motionY, ShipEntity.motionZ)
      sendMessageToTrackedPlayers(velocityMessage)
      ShipEntity.velocityChanged = false
    }
  }

  /*
  private def sendMetadataToAllAssociatedPlayers:Unit = {
    val datawatcher: DataWatcher = ShipEntity.getDataWatcher
    if (datawatcher.hasObjectChanged) {
      // new S1CPacketEntityMetadata(this.trackedEntity.getEntityId, datawatcher, false)
      val dataWatcherMessage = new ShipDatawatcherMessage(ShipEntity,false)
      sendMessageToTrackedPlayers(dataWatcherMessage)
    }
  }
  */
  def updatePlayerEntities(players: Set[EntityPlayerMP]): Unit = players.foreach(updatePlayerEntity)

  def updatePlayerEntity(playerMP: EntityPlayerMP): Unit = {
    if (!withinTrackingDistance(playerMP)) {
      TrackingPlayers.remove(playerMP)
      // Costly to remove the shipworld and then send it back again, consider client side caching
      //playerMP.removeEntity(ShipEntity)
    }
    if (TrackingPlayers.contains(playerMP)) return
    TrackingPlayers.add(playerMP)
    val message = new SpawnShipMessage(ShipEntity, playerMP)
    FlyingShips.flyingShipPacketHandler.INSTANCE.sendTo(message, playerMP)

    lastTrackedEntityMotionX = ShipEntity.motionX
    lastTrackedEntityMotionY = ShipEntity.motionY
    lastTrackedEntityMotionZ = ShipEntity.motionZ

    val velocityMessage = new ShipVelocityMessage(ShipEntity.ShipID, ShipEntity.motionX, ShipEntity.motionY, ShipEntity.motionZ)
    FlyingShips.flyingShipPacketHandler.INSTANCE.sendTo(velocityMessage, playerMP)

  }

  def withinTrackingDistance(playerMP: EntityPlayerMP): Boolean = {
    val closestPos = ShipEntity.Shipworld.getClosestBlockPosToPlayerXZ(playerMP)
    ShipEntity.Shipworld.asInstanceOf[ShipWorldServer].PlayerManager.isPlayerWatchingPos(playerMP, closestPos)
  }
}
