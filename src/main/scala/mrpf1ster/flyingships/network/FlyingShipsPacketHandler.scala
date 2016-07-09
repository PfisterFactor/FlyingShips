package mrpf1ster.flyingships.network

import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side

/**
  * Created by EJ on 3/12/2016.
  */
class FlyingShipsPacketHandler {
  val INSTANCE: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("flyingships")

  INSTANCE.registerMessage(classOf[ClientBlocksChangedMessageHandler], classOf[BlocksChangedMessage], 0, Side.CLIENT)
  INSTANCE.registerMessage(classOf[ClientSpawnShipHandler], classOf[SpawnShipMessage],1,Side.CLIENT)

  INSTANCE.registerMessage(classOf[ServerBlockPlacedMessageHandler], classOf[BlockPlacedMessage],2,Side.SERVER)
  INSTANCE.registerMessage(classOf[ServerBlockDiggingMessageHandler], classOf[BlockDiggingMessage],3,Side.SERVER)


  def registerClientSide() = {

  }

  def registerServerSide() = {

  }

}
