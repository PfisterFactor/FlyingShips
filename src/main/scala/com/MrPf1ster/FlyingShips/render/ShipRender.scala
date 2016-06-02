package com.MrPf1ster.FlyingShips.render

import java.nio.FloatBuffer
import javax.vecmath.Matrix4f

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
import net.minecraft.util.{BlockPos, ResourceLocation}
import org.lwjgl.BufferUtils
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

    // TODO: Render black outlines around blocks

    GL11.glPushMatrix()

    // Translate away from player
    GL11.glTranslated(x, y, z)
    // Translate into the center of the ship block for rotation
    GL11.glTranslated(0.5, 0.5, 0.5)

    // Convert our correction matrix into a buffer to pass to LWJGL (and then to OpenGL)
    val rotationCorrectionBuffer = matrixToFloatBuffer(entity.correctionMatrix)


    // Convert our rotation matrix to a FloatBuffer to pass to LWJGL (and then to OpenGL)
    val rotationBuffer = matrixToFloatBuffer(entity.renderMatrix)

    // Multiply our transformation matrix by the rotation correction
    GL11.glMultMatrix(rotationCorrectionBuffer)
    // Then multiply by the actual rotation
    GL11.glMultMatrix(rotationBuffer)

    // Translate out of the center for drawing the ship
    GL11.glTranslated(-0.5, -0.5, -0.5)


    // Translate to Ship Position
    GL11.glTranslated(entity.ShipBlockPos.getX, entity.ShipBlockPos.getY, entity.ShipBlockPos.getZ)

    RenderHelper.disableStandardItemLighting()
    rm.worldObj = shipWorld
    rm.renderEngine.bindTexture(TextureMap.locationBlocksTexture)

    // Render normal blocks and non-special tile entities
    GL11.glCallList(getDisplayList(shipWorld))
    // Render tile entities that have special renders
    shipWorld.TileEntities
      .filter(pair => TileEntityRendererDispatcher.instance.getSpecialRenderer(pair._2) != null)
      .foreach(pair => {
        def uPos = pair._1
        def te = pair._2
        // If anyone ever wants to explain why the below works i'll be happy to hear
        TileEntityRendererDispatcher.instance.renderTileEntityAt(te, -(uPos.WorldPosX - 2 * uPos.RelPosX), -(uPos.WorldPosY - 2 * uPos.RelPosY), -(uPos.WorldPosZ - 2 * uPos.RelPosZ), partialTicks, -1)
      })

    if (DebugRender.isDebugMenuShown)
      doDebugRender(shipWorld, entityYaw)

    GL11.glPopMatrix()


    DebugRender.drawRotatedBoundingBox(entity.getRelativeRotated, entity, x, y, z)

    RenderHelper.enableStandardItemLighting()


  }

  def getDisplayList(shipWorld: ShipWorld): Int = {
    var id = displayListIDs.get(shipWorld)
    if (id.isDefined && shipWorld.needsRenderUpdate()) {
      GL11.glDeleteLists(id.get,1)
      id = None
      println("Delete List")
    }
    if (id.isEmpty) {
      println("New List")
      // create a new list
      id = Option[Int](GLAllocation.generateDisplayLists( 1 ))
      displayListIDs.put(shipWorld, id.get)

      // build the list
      GL11.glNewList( id.get, GL11.GL_COMPILE )
      renderShip(shipWorld)
      GL11.glEndList()
    }
    id.get
  }

  def renderShip(shipWorld: ShipWorld) = {
    // Setup tessellator and worldRenderer
    def tessellator = Tessellator.getInstance()
    def worldRenderer = tessellator.getWorldRenderer


    def i: Int = shipWorld.Ship.ShipBlockPos.getX
    def j: Int = shipWorld.Ship.ShipBlockPos.getY
    def k: Int = shipWorld.Ship.ShipBlockPos.getZ
    worldRenderer.setTranslation(-i,-j,-k)
    worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK)


    shipWorld.BlockSet.foreach(uPos => {
      val blockState = shipWorld.getBlockState(uPos.RelativePos)
      renderBlock(shipWorld, blockState, uPos.RelativePos, worldRenderer)
    })
    worldRenderer.setTranslation(0,0,0)
    tessellator.draw()


    def renderBlock(shipWorld: ShipWorld, blockState: IBlockState, pos: BlockPos, worldRenderer: WorldRenderer) = {
      // Get the block renderer
      def blockRendererDispatcher = Minecraft.getMinecraft.getBlockRendererDispatcher
      // Get the model of the block
      def bakedModel = blockRendererDispatcher.getModelFromBlockState(blockState, shipWorld, pos)
      // If our block is a normal block
      if (blockState.getBlock.getRenderType == 3)
        blockRendererDispatcher.getBlockModelRenderer.renderModel(shipWorld, bakedModel, blockState, pos, worldRenderer) // Render it
    }
  }


  def doDebugRender(shipWorld: ShipWorld, entityYaw: Float) = {

    GlStateManager.enableBlend()
    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
    GlStateManager.color(1.0F, 0.0F, 0.0F, 0.4F)
    GL11.glLineWidth(4.0F)
    GlStateManager.disableTexture2D()
    GlStateManager.depthMask(false)


    shipWorld.BlockSet.foreach(uPos => {
      val blockState = shipWorld.getBlockState(uPos.RelativePos)
      //DebugRender.debugRenderBlock(shipWorld, blockState, uPos.RelativePos)

    })
    //DebugRender.debugRenderShip(shipWorld.Ship)

    GlStateManager.depthMask(true)
    GlStateManager.enableTexture2D()
    GlStateManager.disableBlend()

  }

  def matrixToFloatBuffer(m: Matrix4f): FloatBuffer = {
    val fb = BufferUtils.createFloatBuffer(16)
    fb.put(m.m00)
    fb.put(m.m01)
    fb.put(m.m02)
    fb.put(m.m03)
    fb.put(m.m10)
    fb.put(m.m11)
    fb.put(m.m12)
    fb.put(m.m13)
    fb.put(m.m20)
    fb.put(m.m21)
    fb.put(m.m22)
    fb.put(m.m23)
    fb.put(m.m30)
    fb.put(m.m31)
    fb.put(m.m32)
    fb.put(m.m33)
    fb.flip()
    return fb;
  }


}