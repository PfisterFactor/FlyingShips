package com.MrPf1ster.FlyingShips.render

import java.nio.FloatBuffer
import javax.vecmath.{Matrix4f, Quat4f}

import com.MrPf1ster.FlyingShips.entities.EntityShip
import com.MrPf1ster.FlyingShips.util.{RenderUtils, RotatedBB}
import com.MrPf1ster.FlyingShips.world.ShipWorld
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.entity.{Render, RenderManager}
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.util.{BlockPos, MovingObjectPosition, ResourceLocation, Vec3}
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11

import scala.collection.mutable.{Map => mMap}


/**
  * Created by EJ on 2/21/2016.
  */


class ShipRender(rm: RenderManager) extends Render[EntityShip](rm) {
  var displayListIDs: mMap[ShipWorld, Int] = mMap()

  // Ships are dynamic, and thus don't have a texture
  override def getEntityTexture(entity: EntityShip): ResourceLocation = null

  // No shadow rendering
  override def doRenderShadowAndFire(entity: Entity, x: Double, y: Double, z: Double, yaw: Float, partialTickTime: Float) = {}


  override def doRender(entity: EntityShip, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) = {

    def shipWorld = entity.ShipWorld



    GL11.glPushMatrix()

    // Translate away from player
    GL11.glTranslated(x, y, z)


    // Translate into the center of the ship block for rotation
    GL11.glTranslated(0.5, 0.5, 0.5)



    // Turn a quaternion into a matrix, then into a FloatBuffer
    val rotationBuffer = matrixToFloatBuffer(quaternionToMatrix4f(entity.Rotation)) //entity.renderMatrix)


    // Multiply current matrix by FloatBuffer
    GL11.glMultMatrix(rotationBuffer)


    // Translate out of the center for drawing the ship
    GL11.glTranslated(-0.5, -0.5, -0.5)


    // Translate to Ship Position
    GL11.glTranslated(entity.posX, entity.posY, entity.posZ)

    RenderHelper.disableStandardItemLighting()

    rm.worldObj = shipWorld
    rm.renderEngine.bindTexture(TextureMap.locationBlocksTexture)


    // Render normal blocks and non-special tile entities
    GL11.glCallList(getDisplayList(shipWorld))

    // Render tile entities that have special renders
    // TODO: Fix Signs
    shipWorld.TileEntities
      .filter(pair => TileEntityRendererDispatcher.instance.getSpecialRenderer(pair._2) != null)
      .foreach(pair => {
        def uPos = pair._1
        def te = pair._2
        te.setWorldObj(shipWorld)
        // If anyone ever wants to explain why the below works i'll be happy to hear
        TileEntityRendererDispatcher.instance.renderTileEntityAt(te, -(uPos.WorldPosX - 2 * uPos.RelPosX), -(uPos.WorldPosY - 2 * uPos.RelPosY), -(uPos.WorldPosZ - 2 * uPos.RelPosZ), partialTicks, -1)
      })




    GL11.glPopMatrix()

    if (DebugRender.isDebugMenuShown)
      doDebugRender(shipWorld, x, y, z)

    // Ray-trace across the ship and return it wrapped in an option
    val rayTrace:Option[MovingObjectPosition] = entity.InteractionHandler.getBlockPlayerIsLookingAt(partialTicks)

    if (rayTrace.isDefined) {
      //println(hoveredBlockOnShip.get)
      def block = rayTrace.get.getBlockPos
      renderBlackOutline(entity, block , x, y, z, partialTicks)
    }

    RenderHelper.enableStandardItemLighting()



  }
  private def getDisplayList(shipWorld: ShipWorld): Int = {
    var id = displayListIDs.get(shipWorld)
    if (id.isDefined && shipWorld.needsRenderUpdate) {
      GL11.glDeleteLists(id.get, 1)
      id = None
      shipWorld.onRenderUpdate()
    }
    if (id.isEmpty) {
      // create a new list
      id = Option[Int](GLAllocation.generateDisplayLists(1))
      displayListIDs.put(shipWorld, id.get)

      // build the list
      GL11.glNewList(id.get, GL11.GL_COMPILE)
      renderShip(shipWorld)
      GL11.glEndList()
    }
    id.get
  }

  private def renderShip(shipWorld: ShipWorld) = {
    // Setup tessellator and worldRenderer
    def tessellator = Tessellator.getInstance()
    def worldRenderer = tessellator.getWorldRenderer

    def i: Double = shipWorld.Ship.posX
    def j: Double = shipWorld.Ship.posY
    def k: Double = shipWorld.Ship.posZ
    worldRenderer.setTranslation(-i, -j, -k)
    worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK)


    shipWorld.BlockSet.foreach(uPos => {
      val blockState = shipWorld.getBlockState(uPos.RelativePos)
      renderBlock(shipWorld, blockState, uPos.RelativePos, worldRenderer)
    })
    worldRenderer.setTranslation(0, 0, 0)
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

  private def renderBlackOutline(ship: EntityShip, pos: BlockPos, x: Double, y: Double, z: Double, partialTicks:Float) = {

    val blockstate = ship.ShipWorld.getBlockState(pos)

    val block = blockstate.getBlock

    block.setBlockBoundsBasedOnState(ship.ShipWorld,pos)

    val aabb = block.getSelectedBoundingBox(ship.ShipWorld,pos)

    val rotatedBB = new RotatedBB(aabb, new Vec3(0.5, 0.5, 0.5), ship.Rotation)

    GL11.glPushMatrix()
    GL11.glTranslated(x, y, z)
    GL11.glTranslated(ship.posX, ship.posY, ship.posZ)
    GL11.glTranslated(pos.getX, pos.getY, pos.getZ)
    GlStateManager.enableBlend()
    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
    GlStateManager.color(0.0F, 0.0F, 0.0F, 0.4F)
    GL11.glLineWidth(2.0F)
    GlStateManager.disableTexture2D()
    GlStateManager.depthMask(false)
    val tessellator: Tessellator = Tessellator.getInstance
    val worldrenderer: WorldRenderer = tessellator.getWorldRenderer
    worldrenderer.setTranslation(-ship.posX - pos.getX, -ship.posY - pos.getY, -ship.posZ - pos.getZ)

    RenderUtils.drawRotatedBB(rotatedBB.expand(0.001), worldrenderer)

    worldrenderer.setTranslation(0, 0, 0)

    GlStateManager.depthMask(true)
    GlStateManager.enableTexture2D()
    GlStateManager.disableBlend()

    GL11.glPopMatrix()

  }


  private def doDebugRender(shipWorld: ShipWorld, x: Double, y: Double, z: Double) = {
    DebugRender.drawRotatedBoundingBox(new RotatedBB(shipWorld.Ship.getBoundingBox.RelativeAABB, new Vec3(0, 0, 0), new Quat4f(0, 0, 0, 1)), shipWorld.Ship, x, y, z)
    shipWorld.BlockSet.foreach(uPos => {
      val blockState = shipWorld.getBlockState(uPos.RelativePos)
      DebugRender.debugRenderBlock(shipWorld, blockState, uPos.RelativePos, x, y, z)

    })

  }

  private def matrixToFloatBuffer(m: Matrix4f): FloatBuffer = {
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
    fb
  }

  private def quaternionToMatrix4f(q: Quat4f): Matrix4f = {
    q.normalize()
    new Matrix4f(1.0f - 2.0f * q.getY * q.getY - 2.0f * q.getZ * q.getZ, 2.0f * q.getX * q.getY - 2.0f * q.getZ * q.getW, 2.0f * q.getX * q.getZ + 2.0f * q.getY * q.getW, 0.0f,
      2.0f * q.getX * q.getY + 2.0f * q.getZ * q.getW, 1.0f - 2.0f * q.getX * q.getX - 2.0f * q.getZ * q.getZ, 2.0f * q.getY * q.getZ - 2.0f * q.getX * q.getW, 0.0f,
      2.0f * q.getX * q.getZ - 2.0f * q.getY * q.getW, 2.0f * q.getY * q.getZ + 2.0f * q.getX * q.getW, 1.0f - 2.0f * q.getX * q.getX - 2.0f * q.getY * q.getY, 0.0f,
      0.0f, 0.0f, 0.0f, 1.0f)
  }


}
