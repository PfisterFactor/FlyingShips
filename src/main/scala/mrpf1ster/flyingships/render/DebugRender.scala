package mrpf1ster.flyingships.render

import javax.vecmath.Quat4f

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

  def debugRenderBlock(shipWorld: ShipWorld, blockState: IBlockState, pos: BlockPos) = {
    // Small delta so we can see the block outline
    val delta = 0.01

    val block = blockState.getBlock
    block.setBlockBoundsBasedOnState(shipWorld, pos)

    val blockBB = block.getSelectedBoundingBox(shipWorld, pos).expand(delta, delta, delta)
    val min = new Vec3(blockBB.minX, blockBB.minY, blockBB.minZ)
    val max = new Vec3(blockBB.maxX, blockBB.maxY, blockBB.maxZ)
    drawRotatedBoundingBox(new RotatedBB(min, max, new Vec3(0.5, 0.5, 0.5), new Quat4f(0, 0, 0, 1)), shipWorld.Ship)

  }

  def drawRotatedBoundingBox(rotatedBB: RotatedBB, shipEntity: EntityShip) = {

    GlStateManager.enableBlend()
    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
    GlStateManager.color(1.0F, 0.0F, 0.0F, 0.4F)
    GL11.glLineWidth(4.0F)
    GlStateManager.disableTexture2D()
    GlStateManager.depthMask(false)


    val tessellator: Tessellator = Tessellator.getInstance
    val worldrenderer: WorldRenderer = tessellator.getWorldRenderer
    worldrenderer.setTranslation(0, 0, 0)

    RenderUtils.drawRotatedBB(rotatedBB, worldrenderer)

    worldrenderer.setTranslation(0, 0, 0)

    GlStateManager.depthMask(true)
    GlStateManager.enableTexture2D()
    GlStateManager.disableBlend()
    GlStateManager.color(0, 0, 0, 0)

  }


}
