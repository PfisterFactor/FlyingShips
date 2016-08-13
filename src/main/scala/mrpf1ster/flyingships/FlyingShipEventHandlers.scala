package mrpf1ster.flyingships

import mrpf1ster.flyingships.entities.{ClickSimulator, EntityShipTracker}
import mrpf1ster.flyingships.render.RenderShip
import mrpf1ster.flyingships.util.ShipLocator
import mrpf1ster.flyingships.world.ShipWorld
import net.minecraft.world.WorldServer
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
class FlyingShipEventHandlers {
  @SubscribeEvent
  def onServerTick(event: ServerTickEvent): Unit = ShipManager.onServerTick(event)

  @SideOnly(Side.CLIENT)
  @SubscribeEvent
  def onClientTick(event: TickEvent.ClientTickEvent): Unit = ShipManager.onClientTick(event)

  @SubscribeEvent
  def playerContainerOpen(event: PlayerOpenContainerEvent): Unit = ShipManager.onContainerOpen(event)

  @SideOnly(Side.CLIENT)
  @SubscribeEvent
  def onMouseClick(event: MouseInputEvent): Unit = ClickSimulator.handleMousePress()

  @SubscribeEvent
  def onPlayerUpdate(event: PlayerTickEvent): Unit = EntityShipTracker.onPlayerUpdate(event)

  @SubscribeEvent
  def onPlayerDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent): Unit = {
    RenderShip.destroyDisplayLists()
    EntityShipTracker.ClientSideShips.clear()
    ShipWorld.ShipMouseOver = ShipWorld.DEFUALTMOUSEOVER
    ShipWorld.ShipMouseOverID = -1
  }

  @SubscribeEvent
  def onChunkLoad(event: ChunkEvent.Load): Unit = {
    val ships = ShipLocator.getShips(event.world)
    ships.foreach(ship => ship.Shipworld.onChunkLoad(event.getChunk.xPosition, event.getChunk.zPosition))
  }

  @SubscribeEvent
  def onChunkUnload(event: ChunkEvent.Unload): Unit = {
    val ships = ShipLocator.getShips(event.world)
    ships.foreach(ship => ship.Shipworld.onChunkUnload(event.getChunk.xPosition, event.getChunk.zPosition))
  }

  @SubscribeEvent
  def onWorldSave(event: WorldEvent.Save): Unit = {
    if (event.world.isRemote) return

    ShipManager.ShipLoadManager.saveShips(event.world.asInstanceOf[WorldServer])
  }

  @SubscribeEvent
  def onWorldUnload(event: WorldEvent.Unload): Unit = {
    if (event.world.isRemote) return

    ShipManager.ShipLoadManager.unloadShips(event.world.asInstanceOf[WorldServer])
  }

  @SubscribeEvent
  def onWorldLoad(event: WorldEvent.Load): Unit = {
    if (event.world.isRemote) return
    ShipManager.ShipLoadManager.loadShips(event.world.asInstanceOf[WorldServer])
  }
}
