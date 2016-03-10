package com.MrPf1ster.FlyingShips.render

import com.MrPf1ster.FlyingShips.ShipWorld
import com.MrPf1ster.FlyingShips.entities.ShipEntity
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.{Tessellator, WorldRenderer}
import net.minecraft.util.{AxisAlignedBB, BlockPos}

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
}
