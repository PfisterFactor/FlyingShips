package com.MrPf1ster.FlyingShips.util

import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.{Tessellator, WorldRenderer}
import net.minecraft.util.AxisAlignedBB

/**
  * Created by EJ on 6/3/2016.
  */
object RenderUtils {
  def drawRotatedBB(rotatedBB: RotatedBB, worldRenderer: WorldRenderer) = {
    val tessellator: Tessellator = Tessellator.getInstance

    worldRenderer.begin(3, DefaultVertexFormats.POSITION)
    worldRenderer.pos(rotatedBB.BackBottomLeft.xCoord, rotatedBB.BackBottomLeft.yCoord, rotatedBB.BackBottomLeft.zCoord).endVertex
    worldRenderer.pos(rotatedBB.BackBottomRight.xCoord, rotatedBB.BackBottomRight.yCoord, rotatedBB.BackBottomRight.zCoord).endVertex
    worldRenderer.pos(rotatedBB.ForwardBottomRight.xCoord, rotatedBB.ForwardBottomRight.yCoord, rotatedBB.ForwardBottomRight.zCoord).endVertex
    worldRenderer.pos(rotatedBB.ForwardBottomLeft.xCoord, rotatedBB.ForwardBottomLeft.yCoord, rotatedBB.ForwardBottomLeft.zCoord).endVertex
    worldRenderer.pos(rotatedBB.BackBottomLeft.xCoord, rotatedBB.BackBottomLeft.yCoord, rotatedBB.BackBottomLeft.zCoord).endVertex
    tessellator.draw
    worldRenderer.begin(3, DefaultVertexFormats.POSITION)
    worldRenderer.pos(rotatedBB.BackTopLeft.xCoord, rotatedBB.BackTopLeft.yCoord, rotatedBB.BackTopLeft.zCoord).endVertex
    worldRenderer.pos(rotatedBB.BackTopRight.xCoord, rotatedBB.BackTopRight.yCoord, rotatedBB.BackTopRight.zCoord).endVertex
    worldRenderer.pos(rotatedBB.ForwardTopRight.xCoord, rotatedBB.ForwardTopRight.yCoord, rotatedBB.ForwardTopRight.zCoord).endVertex
    worldRenderer.pos(rotatedBB.ForwardTopLeft.xCoord, rotatedBB.ForwardTopLeft.yCoord, rotatedBB.ForwardTopLeft.zCoord).endVertex
    worldRenderer.pos(rotatedBB.BackTopLeft.xCoord, rotatedBB.BackTopLeft.yCoord, rotatedBB.BackTopLeft.zCoord).endVertex
    tessellator.draw
    worldRenderer.begin(1, DefaultVertexFormats.POSITION)
    worldRenderer.pos(rotatedBB.BackBottomLeft.xCoord, rotatedBB.BackBottomLeft.yCoord, rotatedBB.BackBottomLeft.zCoord).endVertex
    worldRenderer.pos(rotatedBB.BackTopLeft.xCoord, rotatedBB.BackTopLeft.yCoord, rotatedBB.BackTopLeft.zCoord).endVertex
    worldRenderer.pos(rotatedBB.BackBottomRight.xCoord, rotatedBB.BackBottomRight.yCoord, rotatedBB.BackBottomRight.zCoord).endVertex
    worldRenderer.pos(rotatedBB.BackTopRight.xCoord, rotatedBB.BackTopRight.yCoord, rotatedBB.BackTopRight.zCoord).endVertex
    worldRenderer.pos(rotatedBB.ForwardBottomRight.xCoord, rotatedBB.ForwardBottomRight.yCoord, rotatedBB.ForwardBottomRight.zCoord).endVertex
    worldRenderer.pos(rotatedBB.ForwardTopRight.xCoord, rotatedBB.ForwardTopRight.yCoord, rotatedBB.ForwardTopRight.zCoord).endVertex
    worldRenderer.pos(rotatedBB.ForwardBottomLeft.xCoord, rotatedBB.ForwardBottomLeft.yCoord, rotatedBB.ForwardBottomLeft.zCoord).endVertex
    worldRenderer.pos(rotatedBB.ForwardTopLeft.xCoord, rotatedBB.ForwardTopLeft.yCoord, rotatedBB.ForwardTopLeft.zCoord).endVertex
    tessellator.draw


  }

  def drawAABB(BoundingBox: AxisAlignedBB) {
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
}
