package mrpf1ster.flyingships

import java.io.File

import mrpf1ster.flyingships.entities.{EntityShip, EntityShipTracker}
import mrpf1ster.flyingships.render.RenderShip
import mrpf1ster.flyingships.util.{ShipLocator, UnifiedVec}
import mrpf1ster.flyingships.world.chunk.ClientChunkProviderShip
import mrpf1ster.flyingships.world.{PlayerRelative, ShipWorld, ShipWorldServer}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.{CompressedStreamTools, NBTTagCompound}
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent
import net.minecraftforge.event.world.{ChunkEvent, WorldEvent}
import net.minecraftforge.fml.common.eventhandler.Event.Result
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.MouseInputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.{PlayerTickEvent, ServerTickEvent}
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

import scala.reflect.io.Directory

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
  def onServerTick(event: ServerTickEvent): Unit = ShipManager.onServerTick(event)

  @SideOnly(Side.CLIENT)
  @SubscribeEvent
  def onClientTick(event: TickEvent.ClientTickEvent): Unit = ShipManager.onClientTick(event)

  @SubscribeEvent
  def onPlayerUpdate(event: PlayerTickEvent) = {
    if (!event.player.worldObj.isRemote && event.phase == TickEvent.Phase.END)
      EntityShipTracker.onPlayerUpdate(event.player.asInstanceOf[EntityPlayerMP])
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
    if (leftMouseButtonDown && !doLeftClick) {
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
  def onPlayerDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) = {
    EntityShipTracker.ClientSideShips.clear()
    RenderShip.DisplayListIDs.clear()
    ShipWorld.ShipMouseOver = ShipWorld.DEFUALTMOUSEOVER
    ShipWorld.ShipMouseOverID = -1
  }

  @SubscribeEvent
  def onWorldUnload(event: WorldEvent.Unload): Unit = {
    if (event.world.isRemote) return
    val ships = ShipLocator.getShips(event.world)
    ships.foreach(ship => {
      ship.Shipworld.asInstanceOf[ShipWorldServer].saveAllChunks(true)
      EntityShipTracker.untrackShip(ship)
    })
  }

  // Todo: Abstract these method calls into Shipworld class
  @SubscribeEvent
  def onChunkUnload(event: ChunkEvent.Unload): Unit = event.world.isRemote match {
    case true =>
      val ships = ShipLocator.getShips(event.world)
      ships.foreach(ship => ship.Shipworld.getChunkProvider.asInstanceOf[ClientChunkProviderShip].onWorldChunkUnload(event.getChunk.xPosition, event.getChunk.zPosition))
    case false =>
      val ships = ShipLocator.getShips(event.world)
      ships.foreach(ship => ship.Shipworld.asInstanceOf[ShipWorldServer].PlayerManager.onWorldChunkUnload(event.getChunk.xPosition, event.getChunk.zPosition))
  }

  // Todo: Abstract these method calls into Shipworld class
  @SubscribeEvent
  def onChunkLoad(event: ChunkEvent.Load): Unit = event.world.isRemote match {
    case true =>
      val ships = ShipLocator.getShips(event.world)
      ships.foreach(ship => ship.Shipworld.getChunkProvider.asInstanceOf[ClientChunkProviderShip].onWorldChunkLoad(event.getChunk.xPosition, event.getChunk.zPosition))
    case false =>
      val ships = ShipLocator.getShips(event.world)
      ships.foreach(ship => ship.Shipworld.asInstanceOf[ShipWorldServer].PlayerManager.onWorldChunkLoad(event.getChunk.xPosition, event.getChunk.zPosition))
  }

  @SubscribeEvent
  def onWorldSave(event: WorldEvent.Save) = {
    val ships = ShipLocator.getShips(event.world)
    ships.foreach(ship => {
      if (!ship.isDead && ship.Shipworld != null && ship.Shipworld.isShipValid) {
        try {
          val saveFile = new File(ship.Shipworld.getSaveHandler.getWorldDirectory.getPath + File.separator + "ShipEntity" + event.world.provider.getDimensionId + ".dat")
          val nbt = new NBTTagCompound()
          ship.writeEntityToNBT(nbt)
          CompressedStreamTools.write(nbt, saveFile)
        }
        catch {
          case e: Exception => FlyingShips.logger.warn(s"onWorldSave: Something went wrong writing Ship ID ${ship.ShipID} to file!\n$e")
        }

      }
    })
  }

  @SubscribeEvent
  def onWorldLoad(event: WorldEvent.Load): Unit = {
    if (event.world.isRemote) return
    try {
      val file = new File(event.world.getSaveHandler.getWorldDirectory.getPath + File.separator + "ShipWorlds")
      val saveDirectory = new Directory(file)
      saveDirectory.deepList(2).foreach(path => {
        if (path.name.endsWith("ShipEntity" + event.world.provider.getDimensionId + ".dat")) {
          val nbt = CompressedStreamTools.read(path.jfile)
          if (!nbt.hasNoTags) {
            val entityShip = new EntityShip(event.world)
            entityShip.readEntityFromNBT(nbt)
            EntityShip.addShipToWorld(entityShip)
            EntityShip.maxShipID(entityShip.ShipID)
          }
        }
      })
    }
    catch {
      case e: Exception =>
        FlyingShips.logger.warn(s"onWorldLoad: Something went wrong loading Ships into world!")
        e.printStackTrace(System.out)
    }

  }
}
