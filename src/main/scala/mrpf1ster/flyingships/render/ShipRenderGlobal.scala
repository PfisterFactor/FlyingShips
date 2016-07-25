package mrpf1ster.flyingships.render

import mrpf1ster.flyingships.entities.EffectRendererShip
import mrpf1ster.flyingships.util.{UnifiedPos, UnifiedVec, VectorUtils}
import mrpf1ster.flyingships.world.ShipWorldClient
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.renderer.DestroyBlockProgress
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{BlockPos, EnumParticleTypes, ResourceLocation, Vec3}
import net.minecraft.world.IWorldAccess

import scala.collection.mutable

/**
  * Created by ej on 7/25/16.
  */
case class ShipRenderGlobal(shipWorld: ShipWorldClient) extends IWorldAccess {
  val DamagedBlocks: mutable.Map[Int, DestroyBlockProgress] = mutable.Map()

  // ShipRender handles this
  override def markBlockForUpdate(pos: BlockPos): Unit = {}

  // Pass this on to the Origin world
  // Todo: (Potentially?) fix this for moving ships
  override def playRecord(recordName: String, blockPosIn: BlockPos): Unit = shipWorld.OriginWorld.playRecord(UnifiedPos.convertToWorld(blockPosIn, shipWorld.OriginPos()), recordName)

  // If theres a call to play the block breaking forward it to our effect renderer, pass it to RenderGlobal otherwise
  override def playAuxSFX(player: EntityPlayer, sfxType: Int, blockPosIn: BlockPos, p_180439_4_ : Int): Unit = {

    if (sfxType != 2001) {
      Minecraft.getMinecraft.renderGlobal.playAuxSFX(player, sfxType, UnifiedPos.convertToWorld(blockPosIn, shipWorld.OriginPos()), p_180439_4_)
      return
    }

    val block: Block = Block.getBlockById(p_180439_4_ & 4095)
    val worldRotatedVec = UnifiedVec.convertToWorld(VectorUtils.rotatePointToShip(new Vec3(blockPosIn), shipWorld.Ship), shipWorld.OriginVec())

    if (block.getMaterial != Material.air) {

      Minecraft.getMinecraft.getSoundHandler.playSound(new PositionedSoundRecord(new ResourceLocation(block.stepSound.getBreakSound), (block.stepSound.getVolume + 1.0F) / 2.0F, block.stepSound.getFrequency * 0.8F, worldRotatedVec.xCoord.toFloat + 0.5F, worldRotatedVec.yCoord.toFloat + 0.5F, worldRotatedVec.zCoord.toFloat + 0.5F))
    }
    EffectRendererShip.addBlockDestroyEffects(shipWorld, blockPosIn, block.getStateFromMeta(p_180439_4_ >> 12 & 255))
  }

  // Don't have to track entities in rendering
  override def onEntityAdded(entityIn: Entity): Unit = {}

  override def spawnParticle(particleID: Int, ignoreRange: Boolean, xCoord: Double, yCoord: Double, zCoord: Double, xOffset: Double, yOffset: Double, zOffset: Double, p_180442_15_ : Int*): Unit = {
    val rotatedPos = UnifiedVec.convertToWorld(VectorUtils.rotatePointToShip(new Vec3(xCoord, yCoord, zCoord), shipWorld.Ship), shipWorld.Ship.getPositionVector)
    shipWorld.OriginWorld.spawnParticle(EnumParticleTypes.getParticleFromId(particleID), rotatedPos.xCoord, rotatedPos.yCoord, rotatedPos.zCoord, xOffset + shipWorld.Ship.motionX, yOffset + shipWorld.Ship.motionY, zOffset + shipWorld.Ship.motionZ, 0)
  }

  // Don't do anything
  override def playSound(soundName: String, x: Double, y: Double, z: Double, volume: Float, pitch: Float): Unit = {}

  // Don't do anything
  override def onEntityRemoved(entityIn: Entity): Unit = {}

  // Unless an Enderdragon or Wither is spawned on a shipworld we shouldn't do anything
  override def broadcastSound(p_180440_1_ : Int, p_180440_2_ : BlockPos, p_180440_3_ : Int): Unit = {}

  // Don't do anything
  override def playSoundToNearExcept(except: EntityPlayer, soundName: String, x: Double, y: Double, z: Double, volume: Float, pitch: Float): Unit = {}

  override def markBlockRangeForRenderUpdate(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Unit = shipWorld.doRenderUpdate = true

  override def sendBlockBreakProgress(breakerId: Int, pos: BlockPos, progress: Int): Unit = {
    if (progress >= 0 && progress < 10) {
      var destroyBlockProgress = DamagedBlocks.get(breakerId)
      if (destroyBlockProgress.isEmpty || destroyBlockProgress.get.getPosition.getX != pos.getX || destroyBlockProgress.get.getPosition.getY != pos.getY || destroyBlockProgress.get.getPosition.getZ != pos.getZ) {
        destroyBlockProgress = Some(new DestroyBlockProgress(breakerId, pos))
        DamagedBlocks.put(breakerId, destroyBlockProgress.get)
      }
      destroyBlockProgress.get.setPartialBlockDamage(progress)
      destroyBlockProgress.get.setCloudUpdateTick(0)
    }
    else {
      DamagedBlocks.remove(breakerId)
    }
  }

  // Not completely sure what this does
  // Todo: Fix lighting on ships
  override def notifyLightSet(pos: BlockPos): Unit = {}
}
