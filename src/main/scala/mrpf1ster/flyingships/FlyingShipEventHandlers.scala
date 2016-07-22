package mrpf1ster.flyingships

import mrpf1ster.flyingships.util.{ShipLocator, UnifiedPos}
import mrpf1ster.flyingships.world.PlayerRelative
import mrpf1ster.flyingships.world.chunk.ChunkProviderShip
import net.minecraft.client.Minecraft
import net.minecraft.world.World
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent
import net.minecraftforge.fml.common.eventhandler.Event.Result
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.MouseInputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase

/**
  * Created by EJ on 3/9/2016.
  */
class FlyingShipEventHandlers {

  def updateEntities(world: World): Unit = {


  }

  @SubscribeEvent
  def onServerTick(event: TickEvent.WorldTickEvent): Unit = {
    //Minecraft.getMinecraft.thePlayer.setInvisible(true)
    if (event.phase != Phase.START) return
    ChunkProviderShip.ShipChunkIO.tick
    val ships = ShipLocator.getShips(event.world)
    if (ships.isEmpty) return
    ships.foreach(ship => {
      ship.ShipWorld.tick()
      ship.ShipWorld.updateEntities()
    })

  }

  @SubscribeEvent
  def onClientTick(event: TickEvent.ClientTickEvent): Unit = {
    val ships = ShipLocator.getShips(Minecraft.getMinecraft.theWorld)
    if (ships.isEmpty) return

    if (event.phase == TickEvent.Phase.START && !Minecraft.getMinecraft.isGamePaused) {
      val playerPos = Minecraft.getMinecraft.thePlayer.getPosition
      ships.foreach(ship => {
        val relPlayerPos = UnifiedPos.convertToRelative(playerPos,ship.getPosition)
        ship.ShipWorld.doRandomDisplayTick(relPlayerPos.getX, relPlayerPos.getY, relPlayerPos.getZ)
      })
      return
    }

    ships.foreach(ship => {
      if (Minecraft.getMinecraft.currentScreen != null)
        ship.InteractionHandler.ClickSimulator.leftClickCounter = 10000

      if (ship.InteractionHandler.ClickSimulator.leftClickCounter > 0)
        ship.InteractionHandler.ClickSimulator.leftClickCounter -= 1

      ship.InteractionHandler.ClickSimulator.sendClickBlockToController(Minecraft.getMinecraft.thePlayer)
      if (ship.ShipWorld.isValid)
        ship.ShipWorld.updateEntities()


    })

  }

  var doClick = false
  @SubscribeEvent
  def onMouseLeftClick(event: MouseInputEvent): Unit = {
    val attackIsDown = Minecraft.getMinecraft.gameSettings.keyBindAttack.isKeyDown
    if (!attackIsDown)
      doClick = false

    if (attackIsDown && !doClick)
    {
      if (Minecraft.getMinecraft.theWorld != null)
        ShipLocator.getShips(Minecraft.getMinecraft.theWorld).foreach(ship => ship.InteractionHandler.ClickSimulator.clickMouse(Minecraft.getMinecraft.thePlayer))
    }

    if (attackIsDown)
      doClick = true

  }

  // Hackish way to get the tile entity the player is looking at
  @SubscribeEvent
  def playerContainerOpen(event: PlayerOpenContainerEvent): Unit = {
    val ship = ShipLocator.getShip(Minecraft.getMinecraft.objectMouseOver)

    if (ship.isEmpty) return

    val interactWith = event.entityPlayer.openContainer.canInteractWith(PlayerRelative(event.entityPlayer, ship.get.ShipWorld))
    if (interactWith)
      event.setResult(Result.ALLOW)
    else
      event.setResult(Result.DEFAULT)
  }
}
