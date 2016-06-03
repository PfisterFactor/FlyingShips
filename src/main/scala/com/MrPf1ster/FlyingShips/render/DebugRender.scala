package com.MrPf1ster.FlyingShips.render

import com.MrPf1ster.FlyingShips.ShipWorld
import com.MrPf1ster.FlyingShips.entities.ShipEntity
import com.MrPf1ster.FlyingShips.util.UnifiedBB
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.{GlStateManager, Tessellator, WorldRenderer}
import net.minecraft.util.{AxisAlignedBB, BlockPos}
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
    // println(blockBB)
    Tessellator.getInstance().getWorldRenderer.setTranslation(-shipWorld.Ship.posX, -shipWorld.Ship.posY, -shipWorld.Ship.posZ)
    drawBoundingBox(blockBB)
    Tessellator.getInstance().getWorldRenderer.setTranslation(0, 0, 0)

  }

  def debugRenderShip(shipEntity: ShipEntity) = {
    def bb = shipEntity.BoundingBox.RelativeAABB
    Tessellator.getInstance().getWorldRenderer.setTranslation(-shipEntity.posX, -shipEntity.posY, -shipEntity.posZ)
    drawBoundingBox(bb)
    Tessellator.getInstance().getWorldRenderer.setTranslation(0, 0, 0)
  }


  def drawBoundingBox(BoundingBox: AxisAlignedBB) {
    val tessellator: Tessellator = Tessellator.getInstance
    val worldrenderer: WorldRenderer = tessellator.getWorldRenderer
    worldrenderer.begin(3, DefaultVertexFormats.POSITION)
    worldrenderer.pos(BoundingBox.minX, BoundingBox.minY, BoundingBox.minZ).endVertex
    worldrenderer.pos(BoundingBox.maxX, BoundingBox.minY, BoundingBox.minZ).endVertex
    worldrenderer.pos(BoundingBox.maxX, BoundingBox.minY, BoundingBox.maxZ).endVertex
    worldrenderer.pos(BoundingBox.minX, BoundingBox.minY, BoundingBox.maxZ).endVertex
    worldrenderer.pos(BoundingBox.minX, BoundingBox.minY, BoundingBox.minZ).endVertex
    tessellator.draw
    worldrenderer.begin(3, DefaultVertexFormats.POSITION)
    worldrenderer.pos(BoundingBox.minX, BoundingBox.maxY, BoundingBox.minZ).endVertex
    worldrenderer.pos(BoundingBox.maxX, BoundingBox.maxY, BoundingBox.minZ).endVertex
    worldrenderer.pos(BoundingBox.maxX, BoundingBox.maxY, BoundingBox.maxZ).endVertex
    worldrenderer.pos(BoundingBox.minX, BoundingBox.maxY, BoundingBox.maxZ).endVertex
    worldrenderer.pos(BoundingBox.minX, BoundingBox.maxY, BoundingBox.minZ).endVertex
    tessellator.draw
    worldrenderer.begin(1, DefaultVertexFormats.POSITION)
    worldrenderer.pos(BoundingBox.minX, BoundingBox.minY, BoundingBox.minZ).endVertex
    worldrenderer.pos(BoundingBox.minX, BoundingBox.maxY, BoundingBox.minZ).endVertex
    worldrenderer.pos(BoundingBox.maxX, BoundingBox.minY, BoundingBox.minZ).endVertex
    worldrenderer.pos(BoundingBox.maxX, BoundingBox.maxY, BoundingBox.minZ).endVertex
    worldrenderer.pos(BoundingBox.maxX, BoundingBox.minY, BoundingBox.maxZ).endVertex
    worldrenderer.pos(BoundingBox.maxX, BoundingBox.maxY, BoundingBox.maxZ).endVertex
    worldrenderer.pos(BoundingBox.minX, BoundingBox.minY, BoundingBox.maxZ).endVertex
    worldrenderer.pos(BoundingBox.minX, BoundingBox.maxY, BoundingBox.maxZ).endVertex
    tessellator.draw
  }

  def drawRotatedBoundingBox(rotatedBB: UnifiedBB, shipEntity: ShipEntity, x: Double, y: Double, z: Double) = {

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
    worldrenderer.begin(3, DefaultVertexFormats.POSITION)
    worldrenderer.pos(rotatedBB.BackBottomLeft.RelVecX, rotatedBB.BackBottomLeft.RelVecY, rotatedBB.BackBottomLeft.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.BackBottomRight.RelVecX, rotatedBB.BackBottomRight.RelVecY, rotatedBB.BackBottomRight.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.ForwardBottomRight.RelVecX, rotatedBB.ForwardBottomRight.RelVecY, rotatedBB.ForwardBottomRight.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.ForwardBottomLeft.RelVecX, rotatedBB.ForwardBottomLeft.RelVecY, rotatedBB.ForwardBottomLeft.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.BackBottomLeft.RelVecX, rotatedBB.BackBottomLeft.RelVecY, rotatedBB.BackBottomLeft.RelVecZ).endVertex
    tessellator.draw
    val bbl = rotatedBB.BackBottomLeft.RelativeVec
    val bbr = rotatedBB.BackBottomRight.RelativeVec
    val fbr = rotatedBB.ForwardBottomRight.RelativeVec
    var fbl = rotatedBB.ForwardBottomLeft.RelativeVec
    worldrenderer.begin(3, DefaultVertexFormats.POSITION)
    worldrenderer.pos(rotatedBB.BackTopLeft.RelVecX, rotatedBB.BackTopLeft.RelVecY, rotatedBB.BackTopLeft.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.BackTopRight.RelVecX, rotatedBB.BackTopRight.RelVecY, rotatedBB.BackTopRight.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.ForwardTopRight.RelVecX, rotatedBB.ForwardTopRight.RelVecY, rotatedBB.ForwardTopRight.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.ForwardTopLeft.RelVecX, rotatedBB.ForwardTopLeft.RelVecY, rotatedBB.ForwardTopLeft.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.BackTopLeft.RelVecX, rotatedBB.BackTopLeft.RelVecY, rotatedBB.BackTopLeft.RelVecZ).endVertex
    tessellator.draw
    worldrenderer.begin(1, DefaultVertexFormats.POSITION)
    worldrenderer.pos(rotatedBB.BackBottomLeft.RelVecX, rotatedBB.BackBottomLeft.RelVecY, rotatedBB.BackBottomLeft.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.BackTopLeft.RelVecX, rotatedBB.BackTopLeft.RelVecY, rotatedBB.BackTopLeft.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.BackBottomRight.RelVecX, rotatedBB.BackBottomRight.RelVecY, rotatedBB.BackBottomRight.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.BackTopRight.RelVecX, rotatedBB.BackTopRight.RelVecY, rotatedBB.BackTopRight.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.ForwardBottomRight.RelVecX, rotatedBB.ForwardBottomRight.RelVecY, rotatedBB.ForwardBottomRight.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.ForwardTopRight.RelVecX, rotatedBB.ForwardTopRight.RelVecY, rotatedBB.ForwardTopRight.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.ForwardBottomLeft.RelVecX, rotatedBB.ForwardBottomLeft.RelVecY, rotatedBB.ForwardBottomLeft.RelVecZ).endVertex
    worldrenderer.pos(rotatedBB.ForwardTopLeft.RelVecX, rotatedBB.ForwardTopLeft.RelVecY, rotatedBB.ForwardTopLeft.RelVecZ).endVertex
    tessellator.draw

    Tessellator.getInstance().getWorldRenderer.setTranslation(0, 0, 0)

    GlStateManager.depthMask(true)
    GlStateManager.enableTexture2D()
    GlStateManager.disableBlend()

    GL11.glPopMatrix()
  }
}
