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
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by EJ on 3/6/2016.
  */
object ShipInteractionHandler {
  // Code adapted from https://bitbucket.org/cuchaz/mod-shared/
  // Gets the reach distance of the player
  def getPlayerReachDistance(player: EntityPlayer): Double = player match {
    case playerMP: EntityPlayerMP => playerMP.theItemInWorldManager.getBlockReachDistance
    case _: AbstractClientPlayer => Minecraft.getMinecraft.playerController.getBlockReachDistance()
    case _ => 0

  }
}

case class ShipInteractionHandler(Shipworld: ShipWorld) {

  val ClickSimulator = new ClickSimulator(Shipworld)

  // Gets the block the mouse is over on the passed ship entity
  @SideOnly(Side.CLIENT)
  def getBlockPlayerIsLookingAt(partialTicks: Float): Option[MovingObjectPosition] = {

    // Gets the ship our mouse is over
    val shipMouseOver = ShipWorld.ShipMouseOver
    val shipMouseOverID = ShipWorld.ShipMouseOverID

    if (shipMouseOverID != Shipworld.Ship.getEntityId || shipMouseOver == null || shipMouseOver.typeOfHit != MovingObjectType.BLOCK)
      None
    else
      Some(shipMouseOver)
  }

  @SideOnly(Side.CLIENT)
  def sendBlockDiggingMessage(status: C07PacketPlayerDigging.Action,pos:BlockPos,side:EnumFacing) = {
    val message = new BlockDiggingMessage(status, pos, side, Shipworld.Ship.getEntityId)
    FlyingShips.flyingShipPacketHandler.INSTANCE.sendToServer(message)
  }

  @SideOnly(Side.CLIENT)
  def sendBlockPlacedMessage(pos:BlockPos,side:EnumFacing,heldItem:ItemStack,hitVec:Vec3) = {
    val message = new BlockPlacedMessage(Shipworld.Ship.getEntityId, pos, side.getIndex, heldItem, hitVec)
    FlyingShips.flyingShipPacketHandler.INSTANCE.sendToServer(message)
  }

  def sendBlockPlacedMessage(itemStack: ItemStack) = {
    val message = new BlockPlacedMessage(Shipworld.Ship.getEntityId(), itemStack)
    FlyingShips.flyingShipPacketHandler.INSTANCE.sendToServer(message)
  }


}
