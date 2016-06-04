package com.MrPf1ster.FlyingShips.render

import com.MrPf1ster.FlyingShips.ShipWorld
import com.MrPf1ster.FlyingShips.entities.ShipEntity
import com.MrPf1ster.FlyingShips.util.{RenderUtils, RotatedBB}
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.{GlStateManager, Tessellator, WorldRenderer}
import net.minecraft.util.{BlockPos, Vec3}
import org.lwjgl.opengl.GL11

/**
  * Created by EJ on 3/9/2016.
  */
object DebugRender {

  def isDebugMenuShown = Minecraft.getMinecraft().gameSettings.showDebugInfo

  def debugRenderBlock(shipWorld: ShipWorld, blockState: IBlockState, pos: BlockPos) = {

    // Small delta so we can see the block outline
    val delta = 0.01

    val block = blockState.getBlock
    block.setBlockBoundsBasedOnState(shipWorld, pos)

    val blockBB = block.getSelectedBoundingBox(shipWorld, pos).expand(delta, delta, delta)
    Tessellator.getInstance().getWorldRenderer.setTranslation(-shipWorld.Ship.posX, -shipWorld.Ship.posY, -shipWorld.Ship.posZ)
    val min = new Vec3(blockBB.minX, blockBB.minY, blockBB.minZ)
    val max = new Vec3(blockBB.maxX, blockBB.maxY, blockBB.maxZ)

    RenderUtils.drawRotatedBB(new RotatedBB(min, max, new Vec3(0.5, 0.5, 0.5), shipWorld.Ship.Rotation), Tessellator.getInstance().getWorldRenderer)
    Tessellator.getInstance().getWorldRenderer.setTranslation(0, 0, 0)

  }

  def debugRenderShip(shipEntity: ShipEntity) = {
    def bb = shipEntity.getBoundingBox.RelativeAABB
    Tessellator.getInstance().getWorldRenderer.setTranslation(-shipEntity.posX, -shipEntity.posY, -shipEntity.posZ)
    RenderUtils.drawAABB(bb)
    Tessellator.getInstance().getWorldRenderer.setTranslation(0, 0, 0)
  }




  def drawRotatedBoundingBox(rotatedBB: RotatedBB, shipEntity: ShipEntity, x: Double, y: Double, z: Double) = {

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
    worldrenderer.setTranslation(-shipEntity.posX, -shipEntity.posY, -shipEntity.posZ)

    RenderUtils.drawRotatedBB(rotatedBB, worldrenderer)

    Tessellator.getInstance().getWorldRenderer.setTranslation(0, 0, 0)

    GlStateManager.depthMask(true)
    GlStateManager.enableTexture2D()
    GlStateManager.disableBlend()

    GL11.glPopMatrix()
  }
}
