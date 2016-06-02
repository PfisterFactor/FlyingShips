package com.MrPf1ster.FlyingShips.render

import com.MrPf1ster.FlyingShips.ShipWorld
import com.MrPf1ster.FlyingShips.entities.ShipEntity
import com.MrPf1ster.FlyingShips.util.RotatedBB
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.{GlStateManager, Tessellator, WorldRenderer}
import net.minecraft.util.{AxisAlignedBB, BlockPos, Vec3}
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
    Tessellator.getInstance().getWorldRenderer.setTranslation(-shipWorld.Ship.ShipBlockPos.getX, -shipWorld.Ship.ShipBlockPos.getY, -shipWorld.Ship.ShipBlockPos.getZ)
    drawBoundingBox(blockBB)
    Tessellator.getInstance().getWorldRenderer.setTranslation(0, 0, 0)

  }

  def debugRenderShip(shipEntity: ShipEntity) = {
    def bb = shipEntity.getRelativeBoundingBox
    Tessellator.getInstance().getWorldRenderer.setTranslation(-shipEntity.ShipBlockPos.getX, -shipEntity.ShipBlockPos.getY, -shipEntity.ShipBlockPos.getZ)
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

  def drawRotatedBoundingBox(rotatedBB: RotatedBB, shipEntity: ShipEntity, x: Double, y: Double, z: Double) = {

    GL11.glPushMatrix()
    GL11.glTranslated(x, y, z)
    GL11.glTranslated(shipEntity.ShipBlockPos.getX, shipEntity.ShipBlockPos.getY, shipEntity.ShipBlockPos.getZ)
    GlStateManager.enableBlend()
    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
    GlStateManager.color(1.0F, 0.0F, 0.0F, 0.4F)
    GL11.glLineWidth(4.0F)
    GlStateManager.disableTexture2D()
    GlStateManager.depthMask(false)


    val tessellator: Tessellator = Tessellator.getInstance
    val worldrenderer: WorldRenderer = tessellator.getWorldRenderer
    worldrenderer.setTranslation(-shipEntity.ShipBlockPos.getX, -shipEntity.ShipBlockPos.getY, -shipEntity.ShipBlockPos.getZ)
    worldrenderer.begin(3, DefaultVertexFormats.POSITION)
    worldrenderer.pos(rotatedBB.BackBottomLeft.xCoord, rotatedBB.BackBottomLeft.yCoord, rotatedBB.BackBottomLeft.zCoord).endVertex
    worldrenderer.pos(rotatedBB.BackBottomRight.xCoord, rotatedBB.BackBottomRight.yCoord, rotatedBB.BackBottomRight.zCoord).endVertex
    worldrenderer.pos(rotatedBB.ForwardBottomRight.xCoord, rotatedBB.ForwardBottomRight.yCoord, rotatedBB.ForwardBottomRight.zCoord).endVertex
    worldrenderer.pos(rotatedBB.ForwardBottomLeft.xCoord, rotatedBB.ForwardBottomLeft.yCoord, rotatedBB.ForwardBottomLeft.zCoord).endVertex
    worldrenderer.pos(rotatedBB.BackBottomLeft.xCoord, rotatedBB.BackBottomLeft.yCoord, rotatedBB.BackBottomLeft.zCoord).endVertex
    tessellator.draw
    worldrenderer.begin(3, DefaultVertexFormats.POSITION)
    worldrenderer.pos(rotatedBB.BackTopLeft.xCoord, rotatedBB.BackTopLeft.yCoord, rotatedBB.BackTopLeft.zCoord).endVertex
    worldrenderer.pos(rotatedBB.BackTopRight.xCoord, rotatedBB.BackTopRight.yCoord, rotatedBB.BackTopRight.zCoord).endVertex
    worldrenderer.pos(rotatedBB.ForwardTopRight.xCoord, rotatedBB.ForwardTopRight.yCoord, rotatedBB.ForwardTopRight.zCoord).endVertex
    worldrenderer.pos(rotatedBB.ForwardTopLeft.xCoord, rotatedBB.ForwardTopLeft.yCoord, rotatedBB.ForwardTopLeft.zCoord).endVertex
    worldrenderer.pos(rotatedBB.BackTopLeft.xCoord, rotatedBB.BackTopLeft.yCoord, rotatedBB.BackTopLeft.zCoord).endVertex
    tessellator.draw
    worldrenderer.begin(1, DefaultVertexFormats.POSITION)
    worldrenderer.pos(rotatedBB.BackBottomLeft.xCoord, rotatedBB.BackBottomLeft.yCoord, rotatedBB.BackBottomLeft.zCoord).endVertex
    worldrenderer.pos(rotatedBB.BackTopLeft.xCoord, rotatedBB.BackTopLeft.yCoord, rotatedBB.BackTopLeft.zCoord).endVertex
    worldrenderer.pos(rotatedBB.BackBottomRight.xCoord, rotatedBB.BackBottomRight.yCoord, rotatedBB.BackBottomRight.zCoord).endVertex
    worldrenderer.pos(rotatedBB.BackTopRight.xCoord, rotatedBB.BackTopRight.yCoord, rotatedBB.BackTopRight.zCoord).endVertex
    worldrenderer.pos(rotatedBB.ForwardBottomRight.xCoord, rotatedBB.ForwardBottomRight.yCoord, rotatedBB.ForwardBottomRight.zCoord).endVertex
    worldrenderer.pos(rotatedBB.ForwardTopRight.xCoord, rotatedBB.ForwardTopRight.yCoord, rotatedBB.ForwardTopRight.zCoord).endVertex
    worldrenderer.pos(rotatedBB.ForwardBottomLeft.xCoord, rotatedBB.ForwardBottomLeft.yCoord, rotatedBB.ForwardBottomLeft.zCoord).endVertex
    worldrenderer.pos(rotatedBB.ForwardTopLeft.xCoord, rotatedBB.ForwardTopLeft.yCoord, rotatedBB.ForwardTopLeft.zCoord).endVertex
    tessellator.draw
    Tessellator.getInstance().getWorldRenderer.setTranslation(0, 0, 0)

    GlStateManager.depthMask(true)
    GlStateManager.enableTexture2D()
    GlStateManager.disableBlend()

    GL11.glPopMatrix()
  }
}
