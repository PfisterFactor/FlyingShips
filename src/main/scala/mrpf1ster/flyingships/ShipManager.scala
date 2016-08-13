package mrpf1ster.flyingships

import mrpf1ster.flyingships.entities.{ClickSimulator, EntityShip, EntityShipTracker, ShipInteractionHandler}
import mrpf1ster.flyingships.util.ShipLocator
import mrpf1ster.flyingships.world.chunk.ChunkProviderShip
import mrpf1ster.flyingships.world.{ShipWorld, ShipWorldClient}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.{BlockPos, EnumFacing, MovingObjectPosition, Vec3}
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.{ClientTickEvent, ServerTickEvent}

/**
  * Created by ej on 8/12/16.
  */
object ShipManager {
  def onServerTick(event: ServerTickEvent): Unit = event.phase match {
    case TickEvent.Phase.END =>
      ChunkProviderShip.ShipChunkIO.tick()
      val ships = ShipLocator.getServerShips
      if (ships.isEmpty) return

      ships.foreach(updateShip)

      EntityShipTracker.updateTrackedShips()
    case _ =>

  }

  def onClientTick(event: ClientTickEvent): Unit = event.phase match {
    case TickEvent.Phase.END =>
      if (Minecraft.getMinecraft.isGamePaused) return
      val ships: Set[EntityShip] = ShipLocator.getShips(Minecraft.getMinecraft.theWorld)
      if (ships.isEmpty) return

      val shipMouseOver = getShipMouseOver()
      ShipWorld.ShipMouseOverID = shipMouseOver._1
      ShipWorld.ShipMouseOver = shipMouseOver._2

      ClickSimulator.onClientTick()

      ships.foreach(updateShip)

    case _ =>
  }

  def updateShip(entityShip: EntityShip): Unit = {
    val isClient = entityShip.worldObj.isRemote

    if (entityShip.Shipworld == null) return
    if (entityShip.isDead) {
      if (isClient)
        EntityShipTracker.ClientSideShips.remove(entityShip.ShipID)
      else
        EntityShipTracker.untrackShip(entityShip)
      return
    }
    entityShip.onUpdate()
    if (isClient) {
      entityShip.Shipworld.asInstanceOf[ShipWorldClient].doRandomDisplayTick()
      entityShip.InteractionHandler.ClickSimulator.sendClickBlockToController(Minecraft.getMinecraft.thePlayer)
      if (Minecraft.getMinecraft.gameSettings.keyBindUseItem.isKeyDown && ClickSimulator.rightClickDelayTimer == 0 && !Minecraft.getMinecraft.thePlayer.isUsingItem)
        entityShip.InteractionHandler.ClickSimulator.rightClickMouse()
    }
    if (entityShip.Shipworld.isShipValid) {
      entityShip.Shipworld.tick()
      entityShip.Shipworld.updateEntities()
    }
  }

  //noinspection AccessorLikeMethodIsEmptyParen
  private def getShipMouseOver(): (Int, MovingObjectPosition) = {
    val renderViewEntity = Minecraft.getMinecraft.getRenderViewEntity.asInstanceOf[EntityPlayer]
    val reachDistance = ShipInteractionHandler.getPlayerReachDistance(renderViewEntity)
    val eyePos = renderViewEntity.getPositionEyes(1.0f)
    val lookVector = renderViewEntity.getLookVec
    val ray = eyePos.addVector(lookVector.xCoord * reachDistance, lookVector.yCoord * reachDistance, lookVector.zCoord * reachDistance)
    val aabb = renderViewEntity.getEntityBoundingBox.addCoord(lookVector.xCoord * reachDistance, lookVector.yCoord * reachDistance, lookVector.zCoord * reachDistance).expand(1, 1, 1)

    val shipsInRange = ShipLocator.getShips(Minecraft.getMinecraft.theWorld, aabb)
    // Gets all the ships in range, then sorts by the ships that are closest, then raytraces over all of them, and returns the first raytrace that isn't a miss or null
    val mop = shipsInRange.toList
      .sortBy(ship => ship.getDistanceSqToShipClamped(Minecraft.getMinecraft.thePlayer))
      .map(ship => (ship.ShipID, ship.Shipworld.rayTraceBlocks(eyePos, ray)))
      .find(pair => pair._2 != null && pair._2.typeOfHit != MovingObjectType.MISS)
    if (mop.isEmpty)
      (-1, new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, new Vec3(0, 0, 0), EnumFacing.UP, new BlockPos(-1, -1, -1)))
    else
      mop.get
  }
}
