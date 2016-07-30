package mrpf1ster.flyingships.world

import mrpf1ster.flyingships.util.{UnifiedPos, VectorUtils}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.{S28PacketEffect, S29PacketSoundEffect}
import net.minecraft.server.MinecraftServer
import net.minecraft.util.{BlockPos, Vec3}
import net.minecraft.world.IWorldAccess

/**
  * Created by EJ on 7/29/2016.
  */
// Todo: Implement entities on ships
case class ShipWorldManager(ShipworldServer: ShipWorldServer) extends IWorldAccess {
  override def markBlockForUpdate(pos: BlockPos): Unit = ShipworldServer.PlayerManager.markBlockForUpdate(pos)

  override def playRecord(recordName: String, blockPosIn: BlockPos): Unit = {}

  override def playAuxSFX(player: EntityPlayer, sfxType: Int, blockPosIn: BlockPos, p_180439_4_ : Int): Unit = {
    val worldPos = UnifiedPos.convertToWorld(blockPosIn, ShipworldServer.OriginPos())
    val rotatedVec = VectorUtils.getRotatedWorldVec(new Vec3(blockPosIn), ShipworldServer.Ship)
    MinecraftServer.getServer.getConfigurationManager.sendToAllNearExcept(player, rotatedVec.xCoord, rotatedVec.yCoord, rotatedVec.zCoord, 64.0D, ShipworldServer.OriginWorld.provider.getDimensionId, new S28PacketEffect(sfxType, worldPos, p_180439_4_, false))
  }

  override def onEntityAdded(entityIn: Entity): Unit = {}

  override def spawnParticle(particleID: Int, ignoreRange: Boolean, xCoord: Double, yCoord: Double, zCoord: Double, xOffset: Double, yOffset: Double, zOffset: Double, p_180442_15_ : Int*): Unit = {}

  override def playSound(soundName: String, x: Double, y: Double, z: Double, volume: Float, pitch: Float): Unit = {
    val vol = if (volume > 1.0F) (16.0F * volume).toDouble else 16.0D
    val rotatedVec = VectorUtils.getRotatedWorldVec(x, y, z, ShipworldServer.Ship)
    MinecraftServer.getServer.getConfigurationManager.sendToAllNear(rotatedVec.xCoord, rotatedVec.yCoord, rotatedVec.zCoord, vol, ShipworldServer.OriginWorld.provider.getDimensionId, new S29PacketSoundEffect(soundName, rotatedVec.xCoord, rotatedVec.yCoord, rotatedVec.zCoord, volume, pitch))
  }

  override def onEntityRemoved(entityIn: Entity): Unit = {}

  override def broadcastSound(p_180440_1_ : Int, p_180440_2_ : BlockPos, p_180440_3_ : Int): Unit = {
    val worldPos = UnifiedPos.convertToWorld(p_180440_2_, ShipworldServer.OriginPos())
    MinecraftServer.getServer.getConfigurationManager.sendPacketToAllPlayers(new S28PacketEffect(p_180440_1_, worldPos, p_180440_3_, true))
  }

  override def playSoundToNearExcept(except: EntityPlayer, soundName: String, x: Double, y: Double, z: Double, volume: Float, pitch: Float): Unit = {
    val vol = if (volume > 1.0F) (16.0F * volume).toDouble else 16.0D
    val rotatedVec = VectorUtils.getRotatedWorldVec(x, y, z, ShipworldServer.Ship)
    MinecraftServer.getServer.getConfigurationManager.sendToAllNearExcept(except, rotatedVec.xCoord, rotatedVec.yCoord, rotatedVec.zCoord, vol, ShipworldServer.OriginWorld.provider.getDimensionId, new S29PacketSoundEffect(soundName, rotatedVec.xCoord, rotatedVec.yCoord, rotatedVec.zCoord, volume, pitch))
  }

  override def markBlockRangeForRenderUpdate(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Unit = {}

  // Todo: Implement this
  override def sendBlockBreakProgress(breakerId: Int, pos: BlockPos, progress: Int): Unit = {}

  override def notifyLightSet(pos: BlockPos): Unit = {}
}
