package mrpf1ster.flyingships

import java.io.File

import mrpf1ster.flyingships.entities.{ClickSimulator, EntityShip, EntityShipTracker}
import mrpf1ster.flyingships.util.{ShipLocator, UnifiedVec}
import mrpf1ster.flyingships.world.chunk.ChunkProviderShip
import mrpf1ster.flyingships.world.{PlayerRelative, ShipWorld, ShipWorldClient, ShipWorldServer}
import net.minecraft.client.Minecraft
import net.minecraft.nbt.{CompressedStreamTools, NBTTagCompound}
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.{BlockPos, EnumFacing, MovingObjectPosition, Vec3}
import net.minecraft.world.WorldServer
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent
import net.minecraftforge.fml.common.eventhandler.Event.Result
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.{ClientTickEvent, ServerTickEvent}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

import scala.reflect.io.Directory

/**
  * Created by ej on 8/12/16.
  */
object ShipManager {
  // Called once per tick on server
  def onServerTick(event: ServerTickEvent): Unit = event.phase match {
    case TickEvent.Phase.END =>
      // Does something, unsure what.
      // Related to loading chunks on the ship, I'm sure.
      ChunkProviderShip.ShipChunkIO.tick()
      // Collect our ships to update
      val ships = ShipLocator.getServerShips

      if (ships.isEmpty) return

      // Update all the ships serverside
      ships.foreach(updateShip)

      // Notify clients of any changes in position/rotation/velocity
      EntityShipTracker.updateTrackedShips()
    case _ =>

  }

  // Called once per tick on client
  @SideOnly(Side.CLIENT)
  def onClientTick(event: ClientTickEvent): Unit = event.phase match {
    case TickEvent.Phase.END =>
      // Don't do anything on client if the game is paused
      if (Minecraft.getMinecraft.isGamePaused) return
      // Collect our ships to update
      val ships: Set[EntityShip] = ShipLocator.getShips(Minecraft.getMinecraft.theWorld)
      if (ships.isEmpty) return

      // Raytrace for a block the player is looking at on any shipworlds in front of him/her
      // Returns a -1 id and a miss if nothing found
      val shipMouseOver = getShipMouseOver()
      ShipWorld.ShipMouseOverID = shipMouseOver._1
      ShipWorld.ShipMouseOver = shipMouseOver._2

      // Decrement some counters related to right and left clicking
      ClickSimulator.onClientTick()

      // Update all the ships clientside
      ships.foreach(updateShip)

    case _ =>
  }

  // Hackish way to get the tile entity the player is interacting with
  // Todo: Make this more reliable
  def onContainerOpen(event: PlayerOpenContainerEvent): Unit = {
    var ship: Option[EntityShip] = None

    val ships = ShipLocator.getShips(event.entityPlayer.worldObj)
    // Prep our ships to record any calls to getTileEntity or getBlockState
    ShipWorld.startAccessing()
    // Usually tile entities call getTileEntity or getBlockState to check if they can be interacted with
    event.entityPlayer.openContainer.canInteractWith(event.entityPlayer)
    // Find any ship that has just been accessed
    ship = ships.find(ent => ent.Shipworld != null && ent.Shipworld.wasAccessed)
    // Stop recording calls to getTileEntity or getBlockState
    ShipWorld.stopAccessing(event.entityPlayer.worldObj)

    if (ship.isEmpty) return
    // Resend a canInteractWith call, this time with the player relative to the ship we found
    val interactWith = event.entityPlayer.openContainer.canInteractWith(PlayerRelative(event.entityPlayer, ship.get.Shipworld))
    if (interactWith)
      event.setResult(Result.ALLOW)
    else
      event.setResult(Result.DEFAULT)
  }

  // Does an update on the passed ship
  // Called on every ship in the ship tracker once per tick on client and server
  def updateShip(entityShip: EntityShip): Unit = {
    val isClient = entityShip.worldObj.isRemote
    // Make sure the ship we're working with is set up
    if (entityShip.Shipworld == null) return
    // If the ship is dead, we have to remove it from it's respective tracker
    if (entityShip.isDead) {
      if (isClient)
        EntityShipTracker.ClientSideShips.remove(entityShip.ShipID)
      else
        EntityShipTracker.untrackShip(entityShip)
      return
    }
    // Update the ship's rotation, movement, etc
    entityShip.onUpdate()
    // If we're on client, we have to do some extra stuff
    if (isClient) {
      val shipWorld = entityShip.Shipworld.asInstanceOf[ShipWorldClient]
      // Makes things like furnace fire and bedrock particles work.
      shipWorld.doRandomDisplayTick()
      // Tell our ClickSimulator to check if this ship has to process any left clicks
      ClickSimulator.sendClickBlockToController(shipWorld)
      // Tell our ClickSimulator to check if the ship has to process any right clicks
      if (Minecraft.getMinecraft.gameSettings.keyBindUseItem.isKeyDown && ClickSimulator.rightClickDelayTimer == 0 && !Minecraft.getMinecraft.thePlayer.isUsingItem)
        ClickSimulator.rightClickMouse(shipWorld)
    }
    if (entityShip.Shipworld.isShipValid) {
      // Handles world logic
      entityShip.Shipworld.tick()
      // Updates any entities within the ship, which not implemented... todo: implement entities
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
  @SideOnly(Side.CLIENT)
  def onMouseOverHook() = {
    val mouseOver = Minecraft.getMinecraft.objectMouseOver

    val player = Minecraft.getMinecraft.thePlayer
    // Only do anything if both of them are a hit
    if (mouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.MISS && ShipWorld.ShipMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.MISS) {
      // Convert our block-on-ship hit vector to world coords
      val shipMouseOverVec = UnifiedVec.convertToWorld(ShipWorld.ShipMouseOver.hitVec, ShipLocator.getClientShip(ShipWorld.ShipMouseOverID).get.Shipworld.OriginVec())
      // If objectMouseOver is closer...
      if (player.getDistanceSq(mouseOver.hitVec.xCoord, mouseOver.hitVec.yCoord, mouseOver.hitVec.zCoord) < player.getDistanceSq(shipMouseOverVec.xCoord, shipMouseOverVec.yCoord, shipMouseOverVec.zCoord)) {
        // Then ours is a miss
        ShipWorld.ShipMouseOver = ShipWorld.DEFUALTMOUSEOVER
      }
      else
      // However if ours is closer, then objectMouseOver is a miss
        Minecraft.getMinecraft.objectMouseOver = ShipWorld.DEFUALTMOUSEOVER
    }
  }

  //noinspection AccessorLikeMethodIsEmptyParen
  // Gets any block the player's mouse is over that is on a shipworld
  @SideOnly(Side.CLIENT)
  private def getShipMouseOver(): (Int, MovingObjectPosition) = {
    // The player, or technically anything that has a camera strapped to it
    val renderViewEntity = Minecraft.getMinecraft.getRenderViewEntity
    // The player's reach distance
    val reachDistance = ClickSimulator.ShipInteractionHelper.getPlayerReachDistance(Minecraft.getMinecraft.thePlayer)
    // Eye position
    val eyePos = renderViewEntity.getPositionEyes(1.0f)
    // Where the player (render view entity) is looking
    val lookVector = renderViewEntity.getLookVec
    // Construct a ray based on the max the player can reach and where he's looking,
    //
    //     [] (ray)
    //    /
    //   /
    //  /
    // O (eyePos)
    val ray = eyePos.addVector(lookVector.xCoord * reachDistance, lookVector.yCoord * reachDistance, lookVector.zCoord * reachDistance)
    // Construct a bounding box based on the ray
    //
    //  ___[] (ray)
    // |  / |
    // | /  | [aabb]
    // |/___|
    // O (eyePos)
    val aabb = renderViewEntity.getEntityBoundingBox.addCoord(lookVector.xCoord * reachDistance, lookVector.yCoord * reachDistance, lookVector.zCoord * reachDistance).expand(1, 1, 1)
    // Finds all the ships within that bounding box, usually only one
    val shipsInRange = ShipLocator.getShips(Minecraft.getMinecraft.theWorld, aabb)
    // Gets all the ships in range, then sorts by the ships that are closest, then ray-traces over all of them, and returns the first ray-trace that isn't a miss or null
    val mop = shipsInRange.toList
      .sortBy(ship => ship.getDistanceSqToShipClamped(Minecraft.getMinecraft.thePlayer))
      .map(ship => (ship.ShipID, ship.Shipworld.rayTraceBlocks(eyePos, ray)))
      .find(pair => pair._2 != null && pair._2.typeOfHit != MovingObjectType.MISS)
    if (mop.isEmpty)
      (-1, new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, new Vec3(0, 0, 0), EnumFacing.UP, new BlockPos(-1, -1, -1)))
    else
      mop.get
  }

  // Handles loading and saving our ships to file
  object ShipLoadManager {
    // Saves all the ship entities within the world to file
    // Saves ShipEntity NBT to ".../{WorldName}/ShipWorlds/{ShipworldUUID}/ShipEntity{OriginWorldDimensionID}.dat"
    def saveShips(world: WorldServer) = {
      val ships = ShipLocator.getShips(world)
      ships.foreach(ship => {
        // Don't save the ship if it isn't alive or set up right
        if (!ship.isDead && ship.Shipworld != null && ship.Shipworld.isShipValid) {
          try {
            // Construct our save path
            val saveFile = new File(ship.Shipworld.getSaveHandler.getWorldDirectory.getPath + File.separator + "ShipEntity" + world.provider.getDimensionId + ".dat")
            val nbt = new NBTTagCompound()
            // Write the ShipEntity's data to nbt
            ship.writeEntityToNBT(nbt)
            // Write the ShipEntity to file
            CompressedStreamTools.write(nbt, saveFile)
          }
          catch {
            case e: Exception => FlyingShips.logger.warn(s"onWorldSave: Something went wrong writing Ship ID ${ship.ShipID} to file!\n$e")
          }

        }
      })
    }

    // Loads all the ship entities it can find
    // Loads from ".../{WorldName}/ShipWorlds"
    def loadShips(world: WorldServer) = try {
      // Get our "ShipWorlds" directory
      val file = new File(world.getSaveHandler.getWorldDirectory.getPath + File.separator + "ShipWorlds")
      val saveDirectory = new Directory(file)
      // Look through the folder (max two depth)
      saveDirectory.deepList(2).foreach(path => {
        // If the path ends with ShipEntity plus the dimensionID .dat, it's a match
        if (path.name.endsWith("ShipEntity" + world.provider.getDimensionId + ".dat")) {
          // Read it into an nbt object
          val nbt = CompressedStreamTools.read(path.jfile)
          // If it was read properly
          if (!nbt.hasNoTags) {
            // Create the ship...
            val entityShip = new EntityShip(world)
            // Read the nbt into it...
            entityShip.readEntityFromNBT(nbt)
            // Add it to our ship tracker to notify the clients
            EntityShip.addShipToWorld(entityShip)
            // If the current shipID is greater than what the shipID we have currently is, change it to that
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

    // Cleans up and saves chunks on world unload
    def unloadShips(world: WorldServer) = {
      val ships = ShipLocator.getShips(world)
      ships.foreach(ship => {
        ship.Shipworld.asInstanceOf[ShipWorldServer].saveAllChunks(true)
        EntityShipTracker.untrackShip(ship)
      })
    }
  }

}
