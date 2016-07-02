package com.MrPf1ster.FlyingShips.network

import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side

/**
  * Created by EJ on 3/12/2016.
  */
class FlyingShipsPacketHandler {
  val INSTANCE: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("flyingships")

  INSTANCE.registerMessage(classOf[ClientBlocksChangedMessageHandler], classOf[BlocksChangedMessage], 0, Side.CLIENT)
  INSTANCE.registerMessage(classOf[ServerBlockActivatedMessageHandler], classOf[BlockActivatedMessage],1,Side.SERVER)
  INSTANCE.registerMessage(classOf[ClientSpawnShipHandler], classOf[SpawnShipMessage],2,Side.CLIENT)


  def registerClientSide() = {

  }

  def registerServerSide() = {

  }

}
