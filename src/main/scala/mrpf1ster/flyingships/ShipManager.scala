package mrpf1ster.flyingships

import java.io.File

import mrpf1ster.flyingships.entities.{ClickSimulator, EntityShip, EntityShipTracker, ShipInteractionHandler}
import mrpf1ster.flyingships.util.{ShipLocator, UnifiedVec}
import mrpf1ster.flyingships.world.chunk.ChunkProviderShip
import mrpf1ster.flyingships.world.{PlayerRelative, ShipWorld, ShipWorldClient, ShipWorldServer}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.{CompressedStreamTools, NBTTagCompound}
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.{BlockPos, EnumFacing, MovingObjectPosition, Vec3}
import net.minecraft.world.WorldServer
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent
import net.minecraftforge.fml.common.eventhandler.Event.Result
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.{ClientTickEvent, ServerTickEvent}

import scala.reflect.io.Directory

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

  // Hackish way to get the tile entity the player is interacting with
  // Todo: Make this more reliable
  def onContainerOpen(event: PlayerOpenContainerEvent): Unit = {
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

  // Our coremod hooks into EntityRenderer's getMouseOver method and calls this method
  // This changes the objectMouseOver result to be a Miss if there is a ship in front of the block/entity
  // And vice-versa changes ShipMouseOver to be a Miss if there is a block/entity in front of the ship
  // When trying to implement this via forge events, I found getMouseOver was called twice:
  // Once per tick,
  // and once before rendering.
  // So the only option I saw was ASM
  // Don't hurt me...
  def onMouseOverHook() = {
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

  object ShipLoadManager {
    def saveShips(world: WorldServer) = {
      val ships = ShipLocator.getShips(world)

      ships.foreach(ship => {
        if (!ship.isDead && ship.Shipworld != null && ship.Shipworld.isShipValid) {
          try {
            val saveFile = new File(ship.Shipworld.getSaveHandler.getWorldDirectory.getPath + File.separator + "ShipEntity" + world.provider.getDimensionId + ".dat")
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

    def loadShips(world: WorldServer) = try {
      val file = new File(world.getSaveHandler.getWorldDirectory.getPath + File.separator + "ShipWorlds")
      val saveDirectory = new Directory(file)
      saveDirectory.deepList(2).foreach(path => {
        if (path.name.endsWith("ShipEntity" + world.provider.getDimensionId + ".dat")) {
          val nbt = CompressedStreamTools.read(path.jfile)
          if (!nbt.hasNoTags) {
            val entityShip = new EntityShip(world)
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

    def unloadShips(world: WorldServer) = {
      val ships = ShipLocator.getShips(world)
      ships.foreach(ship => {
        ship.Shipworld.asInstanceOf[ShipWorldServer].saveAllChunks(true)
        EntityShipTracker.untrackShip(ship)
      })
    }
  }

}
