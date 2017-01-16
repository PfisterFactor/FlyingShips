package mrpf1ster.flyingships

import mrpf1ster.flyingships.entities.{ClickSimulator, EntityShipTracker}
import mrpf1ster.flyingships.render.RenderShip
import mrpf1ster.flyingships.util.ShipLocator
import mrpf1ster.flyingships.world.ShipWorld
import net.minecraft.client.Minecraft
import net.minecraft.world.WorldServer
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent
import net.minecraftforge.event.world.{ChunkEvent, WorldEvent}
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.MouseInputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.{PlayerTickEvent, ServerTickEvent}
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by EJ on 3/9/2016.
  */
// Handles forge events and forwards them to the appropriate place or processes them
class FlyingShipEventHandlers {

  // Called once per tick on the server
  // Forwarded to our ShipManager
  @SubscribeEvent
  def onServerTick(event: ServerTickEvent): Unit = ShipManager.onServerTick(event)

  // Called once per tick on the client
  // Forwarded to our ShipManager
  @SideOnly(Side.CLIENT)
  @SubscribeEvent
  def onClientTick(event: TickEvent.ClientTickEvent): Unit = ShipManager.onClientTick(event)

  // Called when rendering a game overlay or something
  // We need it to insert some debug info about our ships
  @SideOnly(Side.CLIENT)
  @SubscribeEvent
  def onRenderGameOverlay(event: RenderGameOverlayEvent.Text): Unit = {
    if (event.`type` == RenderGameOverlayEvent.ElementType.TEXT) {
      // Gets the ship in order of biggest to smallest distance to the player, then flips it so it goes small to big
      val ships = ShipLocator.getClientShips.toArray.sortBy(ship => ship.getDistanceSqToShipClamped(Minecraft.getMinecraft.thePlayer)).reverse
      // Produces a string that looks like this:
      // Active Ship IDs: 1234512, 987421
      // The dropRight cuts the last comma and space off so it looks pretty
      val debugString:String = "Active Ship IDs: " + ships.foldRight("")((Ship,acc) => acc + Ship.ShipID + s": ${Ship.Shipworld.BlocksOnShip.size} blocks, ").dropRight(2)

      event.left.add(debugString)

    }
  }
  // Called basically every tick for some reason I cannot fathom.
  // Also calls when there is a player interacting with a container, that too.
  // Forwarded to our ShipManager
  @SubscribeEvent
  def playerContainerOpen(event: PlayerOpenContainerEvent): Unit = ShipManager.onContainerOpen(event)

  // Called whenever the client pressed a mouse button
  // Forwarded to our ClickSimulator
  @SideOnly(Side.CLIENT)
  @SubscribeEvent
  def onMouseClick(event: MouseInputEvent): Unit = ClickSimulator.handleMousePress()

  // Calls whenever a player is updated.
  // Specifically in EntityPlayerMP's onUpdate method.
  // Forwarded to our EntityShipTracker
  @SubscribeEvent
  def onPlayerUpdate(event: PlayerTickEvent): Unit = EntityShipTracker.onPlayerUpdate(event)

  // Calls whenever a client disconnects from a server, on the client.
  // Cleans up the ship render lists and clientside ships, along with clearing the most recent ray-trace so there's no crash next time he/she joins another server
  @SubscribeEvent
  def onPlayerDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent): Unit = {
    RenderShip.destroyDisplayLists()
    EntityShipTracker.ClientSideShips.clear()
    ShipWorld.ShipMouseOver = ShipWorld.DEFUALTMOUSEOVER
    ShipWorld.ShipMouseOverID = -1
  }

  // Called whenever a chunk is loaded
  // Used to tell any ships that have chunks over that chunk to load as well
  // Easier than maintaining a separate player watched manager list
  @SubscribeEvent
  def onChunkLoad(event: ChunkEvent.Load): Unit = {
    val ships = ShipLocator.getShips(event.world)
    ships.foreach(ship => ship.Shipworld.onChunkLoad(event.getChunk.xPosition, event.getChunk.zPosition))
  }

  // Called whenever a chunk is unloaded
  // Used to tell any ships that have chunks over that chunk to unload as well
  // Easier than maintaining a separate player watched manager list
  @SubscribeEvent
  def onChunkUnload(event: ChunkEvent.Unload): Unit = {
    val ships = ShipLocator.getShips(event.world)
    ships.foreach(ship => ship.Shipworld.onChunkUnload(event.getChunk.xPosition, event.getChunk.zPosition))
  }

  // Called whenever a World is being saved to file
  // We hop on and save our shipworlds as well
  // Ignored on client
  @SubscribeEvent
  def onWorldSave(event: WorldEvent.Save): Unit = {
    if (event.world.isRemote) return

    ShipManager.ShipLoadManager.saveShips(event.world.asInstanceOf[WorldServer])
  }

  // Called whenever a World is being unloaded from memory
  // We unload our ships within that world as well
  // Ignored on client
  @SubscribeEvent
  def onWorldUnload(event: WorldEvent.Unload): Unit = {
    if (event.world.isRemote) return

    ShipManager.ShipLoadManager.unloadShips(event.world.asInstanceOf[WorldServer])
  }

  // Called whenever a World is being loaded from file
  // We hop on and load our shipworlds as well
  // Ignored on client
  @SubscribeEvent
  def onWorldLoad(event: WorldEvent.Load): Unit = {
    if (event.world.isRemote) return
    ShipManager.ShipLoadManager.loadShips(event.world.asInstanceOf[WorldServer])
  }
}
