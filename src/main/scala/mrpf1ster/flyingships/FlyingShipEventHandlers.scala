package mrpf1ster.flyingships

import mrpf1ster.flyingships.entities.{ClickSimulator, EntityShip, EntityShipTracker, ShipInteractionHandler}
import mrpf1ster.flyingships.util.{ShipLocator, UnifiedPos, UnifiedVec}
import mrpf1ster.flyingships.world.chunk.ChunkProviderShip
import mrpf1ster.flyingships.world.{PlayerRelative, ShipWorld, ShipWorldServer}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.server.MinecraftServer
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.{BlockPos, EnumFacing, MovingObjectPosition, Vec3}
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.Event.Result
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.MouseInputEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent
import net.minecraftforge.fml.common.gameevent.{PlayerEvent, TickEvent}
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by EJ on 3/9/2016.
  */
object FlyingShipEventHandlers {
  // Our coremod hooks into onShipMouseOver, here we can change the result of it depending on our ship raytrace
  def getMouseOverHook = {
    val mouseOver = Minecraft.getMinecraft.objectMouseOver

    val player = Minecraft.getMinecraft.thePlayer
    if (mouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.MISS && ShipWorld.ShipMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.MISS) {
      // Find the closest one
      val shipMouseOverVec = UnifiedVec.convertToWorld(ShipWorld.ShipMouseOver.hitVec, ShipLocator.getClientShip(ShipWorld.ShipMouseOverID).get.Shipworld.OriginVec())

      if (player.getDistanceSq(mouseOver.hitVec.xCoord, mouseOver.hitVec.yCoord, mouseOver.hitVec.zCoord) < player.getDistanceSq(shipMouseOverVec.xCoord, shipMouseOverVec.yCoord, shipMouseOverVec.zCoord)) {
        ShipWorld.ShipMouseOver = ShipWorld.DEFUALTMOUSEOVER
      }
      else
        Minecraft.getMinecraft.objectMouseOver = ShipWorld.DEFUALTMOUSEOVER
    }
  }
}
class FlyingShipEventHandlers {
  @SubscribeEvent
  def onServerTick(event: TickEvent.WorldTickEvent): Unit = event.phase match {
    case TickEvent.Phase.START =>
      ChunkProviderShip.ShipChunkIO.tick()
      val ships = ShipLocator.getShips(event.world)
      if (ships.isEmpty) return
      ships.foreach(ship => {
        if (!ship.isDead) {
          if (MinecraftServer.getServer.getTickCounter % 900 == 0)
            ship.Shipworld.asInstanceOf[ShipWorldServer].saveAllChunks(true)

          ship.onUpdate()
          if (ship.Shipworld != null) {
            ship.Shipworld.tick()
            ship.Shipworld.updateEntities()
          }
        }
        else {
          EntityShipTracker.untrackShip(ship)
        }
      })
    case TickEvent.Phase.END =>
      EntityShipTracker.updateTrackedShips()
  }

  @SubscribeEvent
  def onPlayerUpdate(event: PlayerTickEvent) = {
    if (!event.player.worldObj.isRemote && event.phase == TickEvent.Phase.END)
      EntityShipTracker.onPlayerUpdate(event.player.asInstanceOf[EntityPlayerMP])
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
          if (!ship.isDead) {
            ship.onUpdate()
            val relPlayerPos = UnifiedPos.convertToRelative(playerPos, ship.getPosition)
            ship.Shipworld.doRandomDisplayTick(relPlayerPos.getX, relPlayerPos.getY, relPlayerPos.getZ)

            ship.InteractionHandler.ClickSimulator.sendClickBlockToController(Minecraft.getMinecraft.thePlayer)
            if (Minecraft.getMinecraft.gameSettings.keyBindUseItem.isKeyDown && ClickSimulator.rightClickDelayTimer == 0 && !Minecraft.getMinecraft.thePlayer.isUsingItem)
              ship.InteractionHandler.ClickSimulator.rightClickMouse()

            if (ship.Shipworld.isShipValid) {
              ship.Shipworld.tick()
              ship.Shipworld.updateEntities()
            }
          }
          else
            EntityShipTracker.ClientSideShips.remove(ship.ShipID)
        }
      })
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

  def addPlayerToShipPlayerManager(playerMP: EntityPlayerMP) = {
    val ships = ShipLocator.getServerShips
    ships.foreach(ship => {
      if (ship.Shipworld != null && !ship.Shipworld.isRemote) {
        ship.Shipworld.asInstanceOf[ShipWorldServer].PlayerManager.removePlayer(playerMP)
        if (playerMP.worldObj == ship.Shipworld.OriginWorld)
          ship.Shipworld.asInstanceOf[ShipWorldServer].PlayerManager.addPlayer(playerMP)
      }
    })
  }

  def removePlayerFromShipManager(playerMP: EntityPlayerMP) = {
    val ships = ShipLocator.getShips(playerMP.worldObj)
    ships.foreach(ship => if (ship.Shipworld != null) ship.Shipworld.asInstanceOf[ShipWorldServer].PlayerManager.removePlayer(playerMP))
  }

  @SubscribeEvent
  def onEntitySpawn(event: EntityJoinWorldEvent): Unit = event.entity match {
    case playerMP: EntityPlayerMP =>
      addPlayerToShipPlayerManager(playerMP)
      FlyingShips.flyingShipPacketHandler.sendAllShipsToClient(playerMP) // Todo: Fix this so it's within the range of the player
    case ship: EntityShip if !ship.worldObj.isRemote => FlyingShips.flyingShipPacketHandler.sendShipToAllClientsInDimension(ship, ship.worldObj.provider.getDimensionId)
    case _ =>
  }

  @SubscribeEvent
  def onPlayerRespawn(event: PlayerRespawnEvent): Unit = {
    if (event.player.worldObj.isRemote) return

    addPlayerToShipPlayerManager(event.player.asInstanceOf[EntityPlayerMP])
  }

  @SubscribeEvent
  def onPlayerLogOut(event: PlayerEvent.PlayerLoggedOutEvent): Unit = {
    removePlayerFromShipManager(event.player.asInstanceOf[EntityPlayerMP])
  }

  @SubscribeEvent
  def onPlayerDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) = {
    EntityShipTracker.ClientSideShips.keys.foreach(EntityShipTracker.ClientSideShips.remove)
  }

  @SubscribeEvent
  def onWorldUnload(event: WorldEvent.Unload): Unit = {
    if (event.world.isRemote) return
    val ships = ShipLocator.getShips(event.world)
    ships.foreach(ship => ship.Shipworld.asInstanceOf[ShipWorldServer].saveAllChunks(true))
  }
}
