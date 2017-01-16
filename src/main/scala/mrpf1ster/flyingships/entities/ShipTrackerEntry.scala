package mrpf1ster.flyingships.entities

import javax.vecmath.Quat4f

import com.unascribed.lambdanetwork.PendingPacket
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.network._
import mrpf1ster.flyingships.world.ShipWorldServer
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.util.{MathHelper, Vec3}
import net.minecraftforge.fml.common.network.simpleimpl.IMessage

import scala.collection.mutable

/**
  * Created by EJ on 7/29/2016.
  */
case class ShipTrackerEntry(ShipEntity: EntityShip, TrackingRange: Int, TrackingFrequency: Int) {

  var lastPosX: Double = ShipEntity.posX
  var lastPosY: Double = ShipEntity.posX
  var lastPosZ: Double = ShipEntity.posX
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

  def sendPacketToTrackedPlayers(packet: PendingPacket) = TrackingPlayers.foreach(p => packet.to(p))
  def sendMessageToTrackedPlayers(message: IMessage) = TrackingPlayers.foreach(p => FlyingShips.flyingShipPacketHandler.INSTANCE.sendTo(message, p))

  def updatePlayerList(players: List[EntityPlayer]): Unit = {
    if (players.isEmpty) return
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

      val deltaPosX: Double = ShipEntity.posX - lastPosX
      val deltaPosY: Double = ShipEntity.posY - lastPosY
      val deltaPosZ: Double = ShipEntity.posZ - lastPosZ
      var packet: PendingPacket = null
      val sendMoveMsg: Boolean = Math.abs(deltaPosX) >= 4 || Math.abs(deltaPosY) >= 4 || Math.abs(deltaPosZ) >= 4 || this.updateCounter % 60 == 0
      val sendRotMsg: Boolean = !rotation.epsilonEquals(ShipEntity.getRotation, 0.01f)

      if (updateCounter > 0) {
        if (deltaPosX >= -128 && deltaPosX < 128 && deltaPosY >= -128 && deltaPosY < 128 && deltaPosZ >= -128 && deltaPosZ < 128 && this.ticksSinceLastForcedTeleport <= 400) {
          // If both are true don't execute
          if (!sendMoveMsg || !sendRotMsg) {
            if (sendMoveMsg)
              packet = PacketSender.sendShipMovementPacket(ShipEntity.ShipID,Some(ShipEntity.getPositionVector),None,isRelative = true)
            else if (sendRotMsg)
              packet = PacketSender.sendShipMovementPacket(ShipEntity.ShipID,None,Some(ShipEntity.getRotation),isRelative = false)
          }
          else
            packet = PacketSender.sendShipMovementPacket(ShipEntity.ShipID,Some(new Vec3(deltaPosX,deltaPosY,deltaPosZ)),Some(ShipEntity.getRotation),isRelative = true)
        }
        else {
          ticksSinceLastForcedTeleport = 0
          packet = PacketSender.sendShipMovementPacket(ShipEntity.ShipID,Some(ShipEntity.getPositionVector),Some(ShipEntity.getRotation),isRelative = false)
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
        val velocityPacket = PacketSender.sendShipVelocityPacket(ShipEntity.ShipID,lastTrackedEntityMotionX,lastTrackedEntityMotionY,lastTrackedEntityMotionZ)
        sendPacketToTrackedPlayers(velocityPacket)
      }

      if (packet != null)
        sendPacketToTrackedPlayers(packet)

      //sendMetadataToAllAssociatedPlayers()

      if (sendMoveMsg) {
        this.lastPosX = ShipEntity.posX
        this.lastPosY = ShipEntity.posY
        this.lastPosZ = ShipEntity.posZ
      }
      if (sendRotMsg) {
        rotation = ShipEntity.getRotation
      }
    }
    updateCounter += 1

    if (ShipEntity.velocityChanged) {
      val velocityPacket = PacketSender.sendShipVelocityPacket(ShipEntity.ShipID, ShipEntity.motionX, ShipEntity.motionY, ShipEntity.motionZ)
      sendPacketToTrackedPlayers(velocityPacket)
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

    val velocityPacket = PacketSender.sendShipVelocityPacket(ShipEntity.ShipID, ShipEntity.motionX, ShipEntity.motionY, ShipEntity.motionZ)
    velocityPacket.to(playerMP)

  }

  def withinTrackingDistance(playerMP: EntityPlayerMP): Boolean = {
    val closestPos = ShipEntity.Shipworld.getClosestBlockPosToPlayerXZ(playerMP)
    ShipEntity.Shipworld.asInstanceOf[ShipWorldServer].PlayerManager.isPlayerWatchingPos(playerMP, closestPos)
  }
}
