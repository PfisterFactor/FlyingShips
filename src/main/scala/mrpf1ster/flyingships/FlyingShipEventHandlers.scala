package mrpf1ster.flyingships

import mrpf1ster.flyingships.util.{ShipLocator, UnifiedPos}
import net.minecraft.client.Minecraft
import net.minecraft.world.World
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.MouseInputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase

/**
  * Created by EJ on 3/9/2016.
  */
class FlyingShipEventHandlers {

  def updateEntities(world: World): Unit = {
    val ships = ShipLocator.getShips(world)
    if (ships.isEmpty) return

    ships.foreach(ship => ship.ShipWorld.updateEntities())
  }

  @SubscribeEvent
  def onServerTick(event: TickEvent.WorldTickEvent): Unit = {
    //Minecraft.getMinecraft.thePlayer.setInvisible(true)
    if (event.phase != Phase.START) return
    updateEntities(event.world)

  }

  @SubscribeEvent
  def onClientTick(event: TickEvent.ClientTickEvent): Unit = {
    val ships = ShipLocator.getShips(Minecraft.getMinecraft.theWorld)
    if (ships.isEmpty) return

    if (event.phase != TickEvent.Phase.END) {
      val playerPos = Minecraft.getMinecraft.thePlayer.getPosition
      ships.foreach(ship => {
        val relPlayerPos = UnifiedPos.convertToRelative(playerPos,ship.getPosition)
        ship.ShipWorld.doRandomDisplayTick(relPlayerPos.getX,relPlayerPos.getZ,relPlayerPos.getZ)
      })
      return
    }

    ships.foreach(ship => {
      if (Minecraft.getMinecraft.currentScreen != null)
        ship.InteractionHandler.ClickSimulator.leftClickCounter = 10000

      if (ship.InteractionHandler.ClickSimulator.leftClickCounter > 0)
        ship.InteractionHandler.ClickSimulator.leftClickCounter -= 1

      ship.InteractionHandler.ClickSimulator.sendClickBlockToController(Minecraft.getMinecraft.thePlayer)
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
}
