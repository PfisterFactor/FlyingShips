package mrpf1ster.flyingships

import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.util.{ShipLocator, UnifiedPos}
import mrpf1ster.flyingships.world.chunk.ChunkProviderShip
import mrpf1ster.flyingships.world.{PlayerRelative, ShipWorld}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
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

  @SideOnly(Side.CLIENT)
  @SubscribeEvent
  def onClientTick(event: TickEvent.ClientTickEvent): Unit = {
    val ships = ShipLocator.getShips(Minecraft.getMinecraft.theWorld)
    if (ships.isEmpty) return

    if (event.phase == TickEvent.Phase.START && !Minecraft.getMinecraft.isGamePaused) {
      val playerPos = Minecraft.getMinecraft.thePlayer.getPosition
      ships.foreach(ship => {
        if (ship.Shipworld != null) {
          val relPlayerPos = UnifiedPos.convertToRelative(playerPos, ship.getPosition)
          ship.Shipworld.doRandomDisplayTick(relPlayerPos.getX, relPlayerPos.getY, relPlayerPos.getZ)
        }
      })
      return
    }

    ships.foreach(ship => {
      if (Minecraft.getMinecraft.currentScreen != null)
        ship.InteractionHandler.ClickSimulator.leftClickCounter = 10000

      if (ship.InteractionHandler.ClickSimulator.leftClickCounter > 0)
        ship.InteractionHandler.ClickSimulator.leftClickCounter -= 1
      if (ship.Shipworld != null) {
        ship.InteractionHandler.ClickSimulator.sendClickBlockToController(Minecraft.getMinecraft.thePlayer)
        if (ship.Shipworld.isShipValid)
          ship.Shipworld.updateEntities()
      }

    })

  }

  var doClick = false

  @SideOnly(Side.CLIENT)
  @SubscribeEvent
  def onMouseLeftClick(event: MouseInputEvent): Unit = {
    val attackIsDown = Minecraft.getMinecraft.gameSettings.keyBindAttack.isKeyDown
    if (!attackIsDown)
      doClick = false

    if (attackIsDown && !doClick)
    {
      if (Minecraft.getMinecraft.theWorld != null)
        ShipLocator.getShips(Minecraft.getMinecraft.theWorld).foreach(ship => if (ship.Shipworld != null) ship.InteractionHandler.ClickSimulator.clickMouse(Minecraft.getMinecraft.thePlayer))
    }

    if (attackIsDown)
      doClick = true

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
