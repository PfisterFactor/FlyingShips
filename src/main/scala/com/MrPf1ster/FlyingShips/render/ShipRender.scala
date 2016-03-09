package com.MrPf1ster.FlyingShips.render

import com.MrPf1ster.FlyingShips.ShipWorld
import com.MrPf1ster.FlyingShips.entities.ShipEntity
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.entity.{Render, RenderManager}
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.util.{AxisAlignedBB, BlockPos, ResourceLocation}
import org.lwjgl.opengl.GL11

import scala.collection.mutable.{Map => mMap}


/**
  * Created by EJ on 2/21/2016.
  */
class ShipRender(rm:RenderManager) extends Render[ShipEntity](rm) {
  var displayListIDs:  mMap[ShipWorld,Int] = mMap()

  override def getEntityTexture(entity: ShipEntity): ResourceLocation = null // Ships are dynamic, and thus don't have a texture
  override def doRenderShadowAndFire(entity:Entity, x:Double, y:Double, z:Double, yaw:Float,partialTickTime:Float) = {} // No shadow rendering
  override def doRender(entity:ShipEntity,x:Double,y:Double,z:Double,entityYaw:Float,partialTicks:Float) = {
    def shipWorld = entity.ShipWorld
    GL11.glPushMatrix()
    GL11.glTranslated( x, y, z )
    GL11.glRotatef(entityYaw, 0.0f, 1.0f, 0.0f)
    GL11.glTranslated(entity.ShipBlockPos.getX, entity.ShipBlockPos.getY, entity.ShipBlockPos.getZ)

    RenderHelper.disableStandardItemLighting()
    rm.worldObj = shipWorld
    rm.renderEngine.bindTexture(TextureMap.locationBlocksTexture )
    GL11.glCallList(getDisplayList(shipWorld))


    GL11.glPopMatrix()

    GL11.glPushMatrix()
    GL11.glTranslated(x, y, z)
    GL11.glRotatef(entityYaw, 0.0f, 1.0f, 0.0f)
    GL11.glTranslated(entity.ShipBlockPos.getX, entity.ShipBlockPos.getY, entity.ShipBlockPos.getZ)

    GlStateManager.enableBlend
    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
    GlStateManager.color(1.0F, 0.0F, 0.0F, 0.4F)
    GL11.glLineWidth(4.0F)
    GlStateManager.disableTexture2D
    GlStateManager.depthMask(false)

    shipWorld.BlockSet.foreach(blockPos => {
      val blockState = shipWorld.getBlockState(blockPos)
      debugRenderBlock(shipWorld, blockState, blockPos)

    })
    debugRenderShip(entity)
    GlStateManager.depthMask(true)
    GlStateManager.enableTexture2D
    GlStateManager.disableBlend



    GL11.glPopMatrix()




    RenderHelper.enableStandardItemLighting()


  }
  def getDisplayList(shipworld:ShipWorld): Int = {
    var id = displayListIDs.get(shipworld)
    if (id.isDefined && shipworld.needsRenderUpdate) {
      GL11.glDeleteLists(id.get,1)
      id = None
      println("Delete List")
    }
    if (id.isEmpty) {
      println("New List")
      // create a new list
      id = Option[Int](GLAllocation.generateDisplayLists( 1 ))
      displayListIDs.put(shipworld,id.get)

      // build the list
      GL11.glNewList( id.get, GL11.GL_COMPILE )
      renderShip(shipworld)
      GL11.glEndList()
    }
    id.get
  }



  def renderShip(shipWorld: ShipWorld) = {
    // Render the block outlines

    def tessellator = Tessellator.getInstance()
    def worldRenderer = tessellator.getWorldRenderer

    val i: Int = shipWorld.Ship.ShipBlockPos.getX
    val j: Int = shipWorld.Ship.ShipBlockPos.getY
    val k: Int = shipWorld.Ship.ShipBlockPos.getZ
    worldRenderer.setTranslation(-i,-j,-k)
    worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK)


    shipWorld.BlockSet.foreach(blockPos => {
      val blockState = shipWorld.getBlockState(blockPos)
      renderBlock(shipWorld,blockState,blockPos,worldRenderer)
    })
    worldRenderer.setTranslation(0,0,0)
    tessellator.draw()

  }


  def renderBlock(shipWorld: ShipWorld, blockState: IBlockState,pos:BlockPos,worldRenderer: WorldRenderer) = {
    // Get the block renderer
    def blockRendererDispatcher = Minecraft.getMinecraft.getBlockRendererDispatcher
    // Get the model of the block
    def bakedModel = blockRendererDispatcher.getModelFromBlockState(blockState, shipWorld, pos)

    // If the block is a tile entity, don't render it. We'll do that later
    val te = shipWorld.getTileEntity(pos)
    if (te == null || TileEntityRendererDispatcher.instance.getSpecialRenderer(te) == null) {
      // If our block is a normal block
      if (blockState.getBlock.getRenderType == 3)
      // Render it
        blockRendererDispatcher.getBlockModelRenderer.renderModel(shipWorld,bakedModel,blockState,pos,worldRenderer)
    }
  }

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
