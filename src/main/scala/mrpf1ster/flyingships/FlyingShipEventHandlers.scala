package mrpf1ster.flyingships

import mrpf1ster.flyingships.entities.{ClickSimulator, EntityShip, ShipInteractionHandler}
import mrpf1ster.flyingships.util.{ShipLocator, UnifiedPos}
import mrpf1ster.flyingships.world.chunk.ChunkProviderShip
import mrpf1ster.flyingships.world.{PlayerRelative, ShipWorld}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.{BlockPos, EnumFacing, MovingObjectPosition, Vec3}
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent
import net.minecraftforge.fml.common.eventhandler.Event.Result
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.MouseInputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by EJ on 3/9/2016.
  */
class FlyingShipEventHandlers {

  @SubscribeEvent
  def onServerTick(event: TickEvent.WorldTickEvent): Unit = {
    if (event.phase != Phase.START) return
    ChunkProviderShip.ShipChunkIO.tick()
    val ships = ShipLocator.getShips(event.world)
    if (ships.isEmpty) return
    ships.foreach(ship => {
      if (ship.Shipworld != null) {
        ship.Shipworld.tick()
        ship.Shipworld.updateEntities()
      }
    })

  }

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
      .sortBy(ship => ship.getDistanceSqToEntity(Minecraft.getMinecraft.thePlayer))
      .map(ship => (ship.getEntityId, ship.Shipworld.rayTraceBlocks(eyePos, ray)))
      .find(pair => pair._2 != null && pair._2.typeOfHit != MovingObjectType.MISS)
    if (mop.isEmpty)
      (-1, new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, new Vec3(0, 0, 0), EnumFacing.UP, new BlockPos(-1, -1, -1)))
    else
      mop.get
  }

  @SideOnly(Side.CLIENT)
  @SubscribeEvent
  def onClientTick(event: TickEvent.ClientTickEvent): Unit = {
    val ships: Set[EntityShip] = ShipLocator.getShips(Minecraft.getMinecraft.theWorld)
    if (ships.isEmpty) return

    if (event.phase == TickEvent.Phase.START && !Minecraft.getMinecraft.isGamePaused) {
      val shipMouseOver = getShipMouseOver()
      ShipWorld.ShipMouseOverID = shipMouseOver._1
      ShipWorld.ShipMouseOver = shipMouseOver._2

      if (Minecraft.getMinecraft.currentScreen != null)
        ClickSimulator.leftClickCounter = 10000

      if (ClickSimulator.leftClickCounter > 0)
        ClickSimulator.leftClickCounter -= 1

      if (ClickSimulator.rightClickDelayTimer > 0)
        ClickSimulator.rightClickDelayTimer -= 1

      val playerPos = Minecraft.getMinecraft.thePlayer.getPosition
      ships.foreach(ship => {
        if (ship.Shipworld != null) {
          val relPlayerPos = UnifiedPos.convertToRelative(playerPos, ship.getPosition)
          ship.Shipworld.doRandomDisplayTick(relPlayerPos.getX, relPlayerPos.getY, relPlayerPos.getZ)

          ship.InteractionHandler.ClickSimulator.sendClickBlockToController(Minecraft.getMinecraft.thePlayer)
          if (Minecraft.getMinecraft.gameSettings.keyBindUseItem.isKeyDown && ClickSimulator.rightClickDelayTimer == 0 && !Minecraft.getMinecraft.thePlayer.isUsingItem)
            ship.InteractionHandler.ClickSimulator.rightClickMouse()

          if (ship.Shipworld.isShipValid)
            ship.Shipworld.updateEntities()
        }
      })
      return
    }


  }

  var doLeftClick = false
  var doRightClick = false
  @SideOnly(Side.CLIENT)
  @SubscribeEvent
  def onMouseClick(event: MouseInputEvent): Unit = {
    val leftMouseButtonDown = Minecraft.getMinecraft.gameSettings.keyBindAttack.isKeyDown
    val rightMouseButtonDown = Minecraft.getMinecraft.gameSettings.keyBindUseItem.isKeyDown

    if (!leftMouseButtonDown)
      doLeftClick = false
    if (!rightMouseButtonDown)
      doRightClick = false
    val ships = ShipLocator.getShips(Minecraft.getMinecraft.theWorld)
    if (leftMouseButtonDown && !doLeftClick)
    {
      if (Minecraft.getMinecraft.theWorld != null)
        ships.foreach(ship => if (ship.Shipworld != null) ship.InteractionHandler.ClickSimulator.clickMouse(Minecraft.getMinecraft.thePlayer))
    }
    if (rightMouseButtonDown && !doRightClick) {
      if (Minecraft.getMinecraft.theWorld != null)
        ships.foreach(ship => if (ship.Shipworld != null) ship.InteractionHandler.ClickSimulator.rightClickMouse())
    }

    if (leftMouseButtonDown)
      doLeftClick = true
    if (rightMouseButtonDown)
      doRightClick = true

  }

  // Hackish way to get the tile entity the player is interacting with
  @SubscribeEvent
  def playerContainerOpen(event: PlayerOpenContainerEvent): Unit = {
    var ship: Option[EntityShip] = None

    val ships = ShipLocator.getShips(event.entityPlayer.worldObj)
    ShipWorld.startAccessing()
    event.entityPlayer.openContainer.canInteractWith(event.entityPlayer)
    ship = ships.find(ent => ent.Shipworld != null && ent.Shipworld.wasAccessed)
    ShipWorld.stopAccessing(event.entityPlayer.worldObj)

    if (ship.isEmpty) return

    val interactWith = event.entityPlayer.openContainer.canInteractWith(PlayerRelative(event.entityPlayer, ship.get.Shipworld))
    if (interactWith)
      event.setResult(Result.ALLOW)
    else
      event.setResult(Result.DEFAULT)
  }

  @SubscribeEvent
  def onEntitySpawn(event: EntityJoinWorldEvent): Unit = event.entity match {
    case playerMP: EntityPlayerMP => FlyingShips.flyingShipPacketHandler.sendAllShipsToClient(playerMP) // Todo: Fix this so it's within the range of the player
    case ship: EntityShip if !ship.worldObj.isRemote => FlyingShips.flyingShipPacketHandler.sendShipToAllClients(ship)
    case _ =>
  }


}
