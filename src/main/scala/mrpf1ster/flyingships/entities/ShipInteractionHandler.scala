package mrpf1ster.flyingships.entities

import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.network.{BlockDiggingMessage, BlockPlacedMessage}
import mrpf1ster.flyingships.world.ShipWorld
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.{BlockPos, EnumFacing, MovingObjectPosition, Vec3}

/**
  * Created by EJ on 3/6/2016.
  */
case class ShipInteractionHandler(ShipWorld: ShipWorld) {

  val ClickSimulator = new ClickSimulator(ShipWorld)

  // Gets the block the mouse is over on the passed ship entity
  def getBlockPlayerIsLookingAt(partialTicks: Float): Option[MovingObjectPosition] = {

    // Gets the object our mouse is over
    val objectMouseOver = Minecraft.getMinecraft.objectMouseOver

    // If the object isn't an entity or its not the ship entity currently being rendered, exit
    // This doesn't work if you are outside the chunk the entity is stored in
    // Not very good for large ships...
    if (objectMouseOver.typeOfHit != MovingObjectType.ENTITY || !objectMouseOver.entityHit.isEntityEqual(ShipWorld.Ship)) return None

    // The dude
    def player = Minecraft.getMinecraft.thePlayer

    // Gets the player's reach distance
    val blockReachDistance = getPlayerReachDistance(player)


    // The player's eye position, relative to the ship block's center
    val eyePos: Vec3 = player.getPositionEyes(partialTicks)


    // The player's look vector, i.e. where his camera is pointing
    val lookVector: Vec3 = player.getLook(partialTicks)


    // The ray we use for block ray-tracing, just the relative eye position plus the farthest the player can interact with blocks in a direction
    val ray: Vec3 = eyePos.addVector(lookVector.xCoord * blockReachDistance, lookVector.yCoord * blockReachDistance, lookVector.zCoord * blockReachDistance)


    ShipWorld.rotatedRayTrace(eyePos,ray)

  }

  // Ship Right click is trigger when the entity is interacted with
  // Ship Left click is triggered when the player left clicks due to atta
  // This executes on client only
  def onShipRightClick(player: EntityPlayer): Boolean = {
    val hitInfo = getBlockPlayerIsLookingAt(1.0f)

    if (hitInfo.isEmpty) return false

    def pos = hitInfo.get.getBlockPos
    def hitVec = hitInfo.get.hitVec
    def side = hitInfo.get.sideHit

    // Simulates a right click, and sends a packet if anything changed
    val didRightClick = ClickSimulator.simulateRightClick(player,pos,hitVec,side)

    if (didRightClick)
      player.swingItem()

    didRightClick
  }

  def sendBlockDiggingMessage(status: C07PacketPlayerDigging.Action,pos:BlockPos,side:EnumFacing) = {
    val message = new BlockDiggingMessage(status,pos,side,ShipWorld.Ship.getEntityId)
    FlyingShips.flyingShipPacketHandler.INSTANCE.sendToServer(message)
  }
  def sendBlockPlacedMessage(pos:BlockPos,side:EnumFacing,heldItem:ItemStack,hitVec:Vec3) = {
    val message = new BlockPlacedMessage(ShipWorld.Ship.getEntityId,pos,side.getIndex,heldItem, hitVec)
    FlyingShips.flyingShipPacketHandler.INSTANCE.sendToServer(message)
  }

  // Code adapted from https://bitbucket.org/cuchaz/mod-shared/
  // Thank you Cuchaz!
  // Gets the reach distance of the player
  def getPlayerReachDistance(player: EntityPlayer): Double = player match {
    case playerMP: EntityPlayerMP => playerMP.theItemInWorldManager.getBlockReachDistance
    case _: AbstractClientPlayer => Minecraft.getMinecraft.playerController.getBlockReachDistance()
    case _ => 0

  }




}
