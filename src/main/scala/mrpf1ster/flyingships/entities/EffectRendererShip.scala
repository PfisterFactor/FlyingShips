package mrpf1ster.flyingships.entities

import java.util.Random

import mrpf1ster.flyingships.util.{UnifiedVec, VectorUtils}
import mrpf1ster.flyingships.world.ShipWorld
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.particle.EntityDiggingFX
import net.minecraft.util.{BlockPos, EnumFacing, MovingObjectPosition, Vec3}

/**
  * Created by EJ on 7/9/2016.
  */
object EffectRendererShip {
  val rand = new Random()

  def addBlockHitEffects(shipWorld: ShipWorld, pos: BlockPos, target: MovingObjectPosition) {
    val block: Block = shipWorld.getBlockState(pos).getBlock
    if (block != null && !block.addHitEffects(shipWorld, target, Minecraft.getMinecraft.effectRenderer)) {
      addBlockHitEffects(shipWorld, pos, target.sideHit)
    }
  }

  def addBlockHitEffects(shipWorld: ShipWorld, pos: BlockPos, side: EnumFacing) {
    val iblockstate: IBlockState = shipWorld.getBlockState(pos)
    val block: Block = iblockstate.getBlock
    if (block.getRenderType != -1) {
      val i: Int = pos.getX
      val j: Int = pos.getY
      val k: Int = pos.getZ
      val f: Float = 0.1F
      var d0: Double = i.toDouble + this.rand.nextDouble * (block.getBlockBoundsMaxX - block.getBlockBoundsMinX - (f * 2.0F).toDouble) + f.toDouble + block.getBlockBoundsMinX
      var d1: Double = j.toDouble + this.rand.nextDouble * (block.getBlockBoundsMaxY - block.getBlockBoundsMinY - (f * 2.0F).toDouble) + f.toDouble + block.getBlockBoundsMinY
      var d2: Double = k.toDouble + this.rand.nextDouble * (block.getBlockBoundsMaxZ - block.getBlockBoundsMinZ - (f * 2.0F).toDouble) + f.toDouble + block.getBlockBoundsMinZ

      if (side == EnumFacing.DOWN) {
        d1 = j.toDouble + block.getBlockBoundsMinY - f.toDouble
      }
      if (side == EnumFacing.UP) {
        d1 = j.toDouble + block.getBlockBoundsMaxY + f.toDouble
      }
      if (side == EnumFacing.NORTH) {
        d2 = k.toDouble + block.getBlockBoundsMinZ - f.toDouble
      }
      if (side == EnumFacing.SOUTH) {
        d2 = k.toDouble + block.getBlockBoundsMaxZ + f.toDouble
      }
      if (side == EnumFacing.WEST) {
        d0 = i.toDouble + block.getBlockBoundsMinX - f.toDouble
      }
      if (side == EnumFacing.EAST) {
        d0 = i.toDouble + block.getBlockBoundsMaxX + f.toDouble
      }
      val worldVec = UnifiedVec.convertToWorld(VectorUtils.rotatePointToShip(new Vec3(d0, d1, d2), shipWorld.Ship), shipWorld.Ship.getPositionVector)
      val a = new EntityDiggingFX.Factory
      val fx: EntityDiggingFX = a.getEntityFX(0, shipWorld.OriginWorld, worldVec.xCoord, worldVec.yCoord, worldVec.zCoord, 0.0D, 0.0D, 0.0D, Block.getStateId(iblockstate)).asInstanceOf[EntityDiggingFX]
      Minecraft.getMinecraft.effectRenderer.addEffect(fx.func_174846_a(pos).multiplyVelocity(0.2F).multipleParticleScaleBy(0.6F))
    }
  }

  def addBlockDestroyEffects(shipWorld: ShipWorld, pos: BlockPos, state: IBlockState) {
    if (!state.getBlock.isAir(shipWorld, pos) && !state.getBlock.addDestroyEffects(shipWorld, pos, Minecraft.getMinecraft.effectRenderer)) {
      val newState = state.getBlock.getActualState(state, shipWorld, pos)
      val i: Int = 4
      var j: Int = 0
      while (j < i) {

        var k: Int = 0
        while (k < i) {

          var l: Int = 0
          while (l < i) {

            val d0: Double = pos.getX.toDouble + (j.toDouble + 0.5D) / i.toDouble
            val d1: Double = pos.getY.toDouble + (k.toDouble + 0.5D) / i.toDouble
            val d2: Double = pos.getZ.toDouble + (l.toDouble + 0.5D) / i.toDouble
            val worldVec = UnifiedVec.convertToWorld(VectorUtils.rotatePointToShip(new Vec3(d0, d1, d2), shipWorld.Ship), shipWorld.Ship.getPositionVector)
            val a = new EntityDiggingFX.Factory
            val fx = a.getEntityFX(0, shipWorld.OriginWorld, worldVec.xCoord, worldVec.xCoord, worldVec.xCoord, 0, 0, 0, Block.getStateId(newState)).asInstanceOf[EntityDiggingFX]
            Minecraft.getMinecraft.effectRenderer.addEffect(fx.func_174846_a(pos))
            l += 1
          }
          k += 1
        }
        j += 1
      }
    }
  }
}
