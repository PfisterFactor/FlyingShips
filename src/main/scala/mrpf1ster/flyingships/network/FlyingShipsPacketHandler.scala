package mrpf1ster.flyingships.network

import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side

import scala.collection.JavaConversions._

/**
  * Created by EJ on 3/12/2016.
  */
class FlyingShipsPacketHandler {
  val INSTANCE: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("flyingships")

  INSTANCE.registerMessage(classOf[ClientBlockChangedMessageHandler], classOf[BlockChangedMessage], 0, Side.CLIENT)
  INSTANCE.registerMessage(classOf[ClientSpawnShipMessageHandler], classOf[SpawnShipMessage], 1, Side.CLIENT)
  INSTANCE.registerMessage(classOf[ClientBlockActionMessageHandler], classOf[BlockActionMessage], 4, Side.CLIENT)
  INSTANCE.registerMessage(classOf[ClientChunkDataMessageHandler], classOf[ChunkDataMessage], 5, Side.CLIENT)
  INSTANCE.registerMessage(classOf[ClientMultipleBlocksChangedMessageHandler], classOf[MultipleBlocksChangedMessage], 6, Side.CLIENT)
  INSTANCE.registerMessage(classOf[ClientUpdateTileEntityMessageHandler], classOf[UpdateTileEntityMessage], 7, Side.CLIENT)
  INSTANCE.registerMessage(classOf[ClientShipVelocityMessageHandler], classOf[ShipVelocityMessage], 8, Side.CLIENT)
  INSTANCE.registerMessage(classOf[ClientShipRelMoveMessageHandler], classOf[ShipRelMoveMessage], 9, Side.CLIENT)
  INSTANCE.registerMessage(classOf[ClientShipRotMessageHandler], classOf[ShipRotMessage], 10, Side.CLIENT)
  INSTANCE.registerMessage(classOf[ClientShipMoveRotMessageHandler], classOf[ShipMoveRotMessage], 11, Side.CLIENT)
  INSTANCE.registerMessage(classOf[ClientDeleteShipMessageHandler], classOf[DeleteShipMessage], 12, Side.CLIENT)

  INSTANCE.registerMessage(classOf[ServerBlockPlacedMessageHandler], classOf[BlockPlacedMessage],2,Side.SERVER)
  INSTANCE.registerMessage(classOf[ServerBlockDiggingMessageHandler], classOf[BlockDiggingMessage],3,Side.SERVER)

  def registerClientSide() = {

  }

  def registerServerSide() = {

  }

  def sendAllShipsToClient(playerMP: EntityPlayerMP): Boolean = {
    if (playerMP.worldObj == null || playerMP.worldObj.isRemote) return false
    def sendShipToClient(ship: EntityShip) = FlyingShips.flyingShipPacketHandler.INSTANCE.sendTo(new SpawnShipMessage(ship, playerMP), playerMP)
    ShipLocator.getShips(playerMP.worldObj).foreach(sendShipToClient)
    true
  }

  def sendShipToAllClientsInDimension(ship: EntityShip, dim: Int): Boolean = {
    if (ship == null || ship.Shipworld == null || ship.Shipworld.isRemote) return false
    DimensionManager.getWorld(dim).playerEntities.foreach(player => {
      FlyingShips.flyingShipPacketHandler.INSTANCE.sendTo(new SpawnShipMessage(ship, player.asInstanceOf[EntityPlayerMP]), player.asInstanceOf[EntityPlayerMP])
    })

    true
  }

  def nullCheck(ship: Option[EntityShip], caller: String, shipID: Int): Boolean = {
    if (ship.isEmpty) {
      FlyingShips.logger.warn(s"$caller: Ship ID ${shipID} was not located! Aborting!")
      return false
    }
    if (ship.get.Shipworld == null) {
      FlyingShips.logger.warn(s"$caller: Ship ID ${shipID}'s Shipworld was null! Aborting!")
      return false
    }
    true
  }


}
