package mrpf1ster.flyingships.render

import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.util.{RenderUtils, RotatedBB}
import mrpf1ster.flyingships.world.ShipWorld
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.{GlStateManager, Tessellator, WorldRenderer}
import net.minecraft.util.{BlockPos, Vec3}
import org.lwjgl.opengl.GL11

/**
  * Created by EJ on 3/9/2016.
  */
object DebugRender {

  def isDebugMenuShown = Minecraft.getMinecraft.gameSettings.showDebugInfo

  def debugRenderBlock(shipWorld: ShipWorld, blockState: IBlockState, pos: BlockPos, x: Double, y: Double, z: Double) = {
    // Small delta so we can see the block outline
    val delta = 0.01

    val block = blockState.getBlock
    block.setBlockBoundsBasedOnState(shipWorld, pos)

    val blockBB = block.getSelectedBoundingBox(shipWorld, pos).expand(delta, delta, delta)
    val min = new Vec3(blockBB.minX, blockBB.minY, blockBB.minZ)
    val max = new Vec3(blockBB.maxX, blockBB.maxY, blockBB.maxZ)
    drawRotatedBoundingBox(new RotatedBB(min, max, new Vec3(0.5, 0.5, 0.5), shipWorld.Ship.getRotation), shipWorld.Ship, x, y, z)

  }

  def drawRotatedBoundingBox(rotatedBB: RotatedBB, shipEntity: EntityShip, x: Double, y: Double, z: Double) = {

    GL11.glPushMatrix()
    GL11.glTranslated(x, y, z)
    GL11.glTranslated(shipEntity.posX, shipEntity.posY, shipEntity.posZ)
    GlStateManager.enableBlend()
    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
    GlStateManager.color(1.0F, 0.0F, 0.0F, 0.4F)
    GL11.glLineWidth(4.0F)
    GlStateManager.disableTexture2D()
    GlStateManager.depthMask(false)


    val tessellator: Tessellator = Tessellator.getInstance
    val worldrenderer: WorldRenderer = tessellator.getWorldRenderer
    worldrenderer.setTranslation(-shipEntity.posX - ShipWorld.ShipBlockPos.getX, -shipEntity.posY - ShipWorld.ShipBlockPos.getY, -shipEntity.posZ - ShipWorld.ShipBlockPos.getZ)

    RenderUtils.drawRotatedBB(rotatedBB, worldrenderer)

    worldrenderer.setTranslation(0, 0, 0)

    GlStateManager.depthMask(true)
    GlStateManager.enableTexture2D()
    GlStateManager.disableBlend()

    GL11.glPopMatrix()
  }


}
