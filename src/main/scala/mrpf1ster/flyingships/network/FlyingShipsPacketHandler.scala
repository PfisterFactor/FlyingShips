package mrpf1ster.flyingships.network

import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side

/**
  * Created by EJ on 3/12/2016.
  */
class FlyingShipsPacketHandler {
  val INSTANCE: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("flyingships")

  INSTANCE.registerMessage(classOf[ClientBlockChangedMessageHandler], classOf[BlockChangedMessage], 0, Side.CLIENT)
  INSTANCE.registerMessage(classOf[ClientSpawnShipHandler], classOf[SpawnShipMessage],1,Side.CLIENT)
  INSTANCE.registerMessage(classOf[ClientBlockActionMessageHandler], classOf[BlockActionMessage], 4, Side.CLIENT)

  INSTANCE.registerMessage(classOf[ServerBlockPlacedMessageHandler], classOf[BlockPlacedMessage],2,Side.SERVER)
  INSTANCE.registerMessage(classOf[ServerBlockDiggingMessageHandler], classOf[BlockDiggingMessage],3,Side.SERVER)

  def registerClientSide() = {

  }

  def registerServerSide() = {

  }

  def sendAllShipsToClient(playerMP: EntityPlayerMP): Boolean = {
    if (playerMP.worldObj == null || playerMP.worldObj.isRemote) return false
    def sendShipToClient(ship: EntityShip) = FlyingShips.flyingShipPacketHandler.INSTANCE.sendTo(new SpawnShipMessage(ship), playerMP)
    ShipLocator.getShips(playerMP.worldObj).foreach(sendShipToClient)
    true
  }

  def sendShipToAllClientsInDimension(ship: EntityShip, dim: Int): Boolean = {
    if (ship == null || ship.Shipworld == null || ship.Shipworld.isRemote) return false

    FlyingShips.flyingShipPacketHandler.INSTANCE.sendToDimension(new SpawnShipMessage(ship), dim)
    true
  }


}
