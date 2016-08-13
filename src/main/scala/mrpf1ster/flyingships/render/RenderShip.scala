package mrpf1ster.flyingships.render

import java.nio.FloatBuffer
import javax.vecmath.{Matrix4f, Quat4f}

import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.util.{RenderUtils, RotatedBB, ShipLocator}
import mrpf1ster.flyingships.world.{ShipWorld, ShipWorldClient}
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.client.renderer.entity.{Render, RenderManager}
import net.minecraft.client.renderer.texture.{TextureAtlasSprite, TextureMap}
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{BlockPos, MovingObjectPosition, ResourceLocation, Vec3}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11

import scala.collection.JavaConversions._
import scala.collection.mutable.{Map => mMap}


/**
  * Created by EJ on 2/21/2016.
  */
@SideOnly(Side.CLIENT)
object RenderShip {
  private def texturemap = Minecraft.getMinecraft.getTextureMapBlocks

  val DestroyBlockIcons: Array[TextureAtlasSprite] = Array.range(0, 10).map(i => texturemap.getAtlasSprite(s"minecraft:blocks/destroy_stage_$i"))
  val DisplayListIDs: mMap[Int, Int] = mMap()

  // We render ships here to get rid of the reliance on the chunk the entity is in
  // Called from our coremod, which hooks into RenderGlobal renderEntities right before every other entity is rendered
  def onRender(partialTicks: Float, camera: ICamera, x: Double, y: Double, z: Double): Unit = {
    if (Minecraft.getMinecraft.theWorld == null) return
    val renderViewEntity = Minecraft.getMinecraft.getRenderViewEntity
    val pass = net.minecraftforge.client.MinecraftForgeClient.getRenderPass

    val shipsToRender = ShipLocator.getClientShips.filter(ship => {
      val shipRender = Minecraft.getMinecraft.getRenderManager.getEntityRenderObject(ship).asInstanceOf[RenderShip]
      if (shipRender != null)
        ship.shouldRenderInPassOverride(pass) && shipRender.shouldRender(ship, camera, x, y, z)
      else
        false
    }).foreach(ship => Minecraft.getMinecraft.getRenderManager.renderEntitySimple(ship, partialTicks))
  }

  // Clear is a better word...
  // But I want to annihilate those display lists.
  def destroyDisplayLists() = {
    val task = new Runnable {
      override def run(): Unit = {
        DisplayListIDs.foreach(pair =>
          try {
            GL11.glDeleteLists(pair._2, 1)
          }
          catch {
            case ex: Exception => FlyingShips.logger.warn(s"RenderShip: There was a problem while destroying Ship ID: ${pair._1}'s display list (id: ${pair._2})! {${ex.getLocalizedMessage}}")
          })
        FlyingShips.logger.info(s"RenderShip: Purged ${DisplayListIDs.size} display list(s).")
        DisplayListIDs.clear()
      }
    }
    Minecraft.getMinecraft.addScheduledTask(task)
  }

}

@SideOnly(Side.CLIENT)
class RenderShip(rm: RenderManager) extends Render[EntityShip](rm) {


  // Ships are dynamic, and thus don't have a texture
  override def getEntityTexture(entity: EntityShip): ResourceLocation = null

  // No shadow rendering (yet!)
  override def doRenderShadowAndFire(entity: Entity, x: Double, y: Double, z: Double, yaw: Float, partialTickTime: Float) = {}


  override def doRender(entity: EntityShip, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) = {

    val shipWorld = entity.Shipworld.asInstanceOf[ShipWorldClient]

    // Adds one to the frame counter in entity
    // Used for determining the number of frames in between rotation sync from the server
    // The client then interpolates the rotation to match the servers rotation
    entity.IncrementFrameCounter()

    GL11.glPushMatrix()

    // Translate away from player
    GL11.glTranslated(x, y, z)

    // Translate into the center of the ship block for rotation
    GL11.glTranslated(0.5, 0.5, 0.5)

    // Interpolate our rotation
    val delta = 1.0 / entity.FramesBetweenLastServerSync
    if (entity.FramesBetweenLastServerSync > 0)
      entity.interpolatedRotation.interpolate(entity.getRotation, delta.toFloat)


    // Turn a quaternion into a matrix, then into a FloatBuffer
    val rotationBuffer = matrixToFloatBuffer(quaternionToMatrix4f(entity.interpolatedRotation))

    // Multiply current matrix by FloatBuffer
    GL11.glMultMatrix(rotationBuffer)


    // Translate out of the center for drawing the ship
    GL11.glTranslated(-0.5, -0.5, -0.5)


    // Translate away from the ShipBlock
    GL11.glTranslated(-ShipWorld.ShipBlockPos.getX, -128, -ShipWorld.ShipBlockPos.getZ)

    RenderHelper.disableStandardItemLighting()

    rm.worldObj = shipWorld
    rm.renderEngine.bindTexture(TextureMap.locationBlocksTexture)

    // Render normal blocks and non-special tile entities
    GL11.glCallList(getDisplayList(shipWorld))

    // Render tile entities that have special renders
    // TODO: Fix Signs
    shipWorld.loadedTileEntityList
      .filter(te => TileEntityRendererDispatcher.instance.getSpecialRenderer(te) != null)
      .foreach(te => {
        te.setWorldObj(shipWorld)
        TileEntityRendererDispatcher.instance.renderTileEntityAt(te, te.getPos.getX, te.getPos.getY, te.getPos.getZ, partialTicks, -1)
      })

    // Block breaking textures
    renderBlockBreaking(shipWorld)

    // Ray-trace across the ship and return it wrapped in an option
    val rayTrace: Option[MovingObjectPosition] = entity.InteractionHandler.getBlockPlayerIsLookingAt(partialTicks)

    if (rayTrace.isDefined) {
      def block = rayTrace.get.getBlockPos
      renderSelectionBox(entity, block)
    }

    // Render stuff that's helpful to visualize the ships collision boxes
    if (DebugRender.isDebugMenuShown)
      doDebugRender(shipWorld)

    RenderHelper.enableStandardItemLighting()

    GL11.glPopMatrix()
  }

  // Gets our ship OpenGL display list
  // Not entirely sure how this works
  // I think it just stores the render in memory and then calls upon it for re-render instead of regenerating it
  // Useful for not destroying frames per sec
  private def getDisplayList(shipWorld: ShipWorldClient): Int = {
    var id = RenderShip.DisplayListIDs.get(shipWorld.Ship.ShipID)
    if (id.isDefined && shipWorld.doRenderUpdate) {
      GL11.glDeleteLists(id.get, 1)
      id = None
      shipWorld.doRenderUpdate = false
    }
    if (id.isEmpty) {
      // Create a new list
      id = Option[Int](GLAllocation.generateDisplayLists(1))
      RenderShip.DisplayListIDs.put(shipWorld.Ship.ShipID, id.get)
      // Build the list
      GL11.glNewList(id.get, GL11.GL_COMPILE)
      renderShip(shipWorld)
      GL11.glEndList()
    }
    id.get
  }

  // This function renders the blocks and liquids on the ship
  // It does not render Tile Entities that have special renderer's however
  private def renderShip(shipWorld: ShipWorldClient) = {
    // Setup tessellator and worldRenderer
    def tessellator = Tessellator.getInstance()
    def worldRenderer = tessellator.getWorldRenderer

    worldRenderer.setTranslation(0, 0, 0)
    worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK)

    shipWorld.BlocksOnShip.foreach(uPos => {
      if (shipWorld.isBlockLoaded(uPos.RelativePos, true)) {
        val blockState = shipWorld.getBlockState(uPos.RelativePos)
        val blockDispatcher = Minecraft.getMinecraft.getBlockRendererDispatcher
        // Renders blocks (and liquids) based on their state
        blockDispatcher.renderBlock(blockState, uPos.RelativePos, shipWorld, worldRenderer)
      }
    })
    tessellator.draw()
  }

  // This function renders all the block breaking textures on the ship
  private def renderBlockBreaking(shipWorld: ShipWorldClient): Unit = {
    // This function renders a block breaking texture on a single block
    def renderBlockBreak(destroyBlockProgress: DestroyBlockProgress, shipWorldClient: ShipWorldClient): Unit = {
      val blockpos: BlockPos = destroyBlockProgress.getPosition
      val d3: Double = blockpos.getX.toDouble - ShipWorld.ShipBlockVec.xCoord
      val d4: Double = blockpos.getY.toDouble - ShipWorld.ShipBlockVec.yCoord
      val d5: Double = blockpos.getZ.toDouble - ShipWorld.ShipBlockVec.zCoord
      val block: Block = shipWorldClient.getBlockState(blockpos).getBlock
      val te: TileEntity = shipWorldClient.getTileEntity(blockpos)
      var hasBreak: Boolean = block.isInstanceOf[BlockChest] || block.isInstanceOf[BlockEnderChest] || block.isInstanceOf[BlockSign] || block.isInstanceOf[BlockSkull]
      if (!hasBreak) hasBreak = te != null && te.canRenderBreaking
      if (!hasBreak) {
        if (d3 * d3 + d4 * d4 + d5 * d5 > 1024.0D)
          return
        val iblockstate: IBlockState = shipWorldClient.getBlockState(blockpos)
        if (iblockstate.getBlock.getMaterial != Material.air) {
          val i: Int = destroyBlockProgress.getPartialBlockDamage
          val textureatlassprite: TextureAtlasSprite = RenderShip.DestroyBlockIcons(i)
          val blockrendererdispatcher: BlockRendererDispatcher = Minecraft.getMinecraft.getBlockRendererDispatcher
          blockrendererdispatcher.renderBlockDamage(iblockstate, blockpos, textureatlassprite, shipWorldClient)
        }
      }
    }

    if (shipWorld.ShipRenderGlobal.DamagedBlocks.isEmpty) return
    GlStateManager.tryBlendFuncSeparate(774, 768, 1, 0)
    GlStateManager.enableBlend()
    GlStateManager.color(1.0F, 1.0F, 1.0F, 0.5F)
    GlStateManager.doPolygonOffset(-3.0F, -3.0F)
    GlStateManager.enablePolygonOffset()
    GlStateManager.alphaFunc(516, 0.1F)
    GlStateManager.enableAlpha()

    val tessellator = Tessellator.getInstance()
    val worldRenderer = tessellator.getWorldRenderer

    val vec = shipWorld.Ship.getPositionVector.add(ShipWorld.ShipBlockVec)

    worldRenderer.begin(7, DefaultVertexFormats.BLOCK)
    worldRenderer.setTranslation(0, 0, 0)
    worldRenderer.noColor()
    shipWorld.ShipRenderGlobal.DamagedBlocks.foreach(pair => renderBlockBreak(pair._2, shipWorld))
    tessellator.draw()


    GlStateManager.disableAlpha()
    GlStateManager.doPolygonOffset(0.0F, 0.0F)
    GlStateManager.disablePolygonOffset()
    GlStateManager.enableAlpha()
    GlStateManager.depthMask(true)
    GlStateManager.disableBlend()
  }

  // Renders a selection box around a given position
  private def renderSelectionBox(ship: EntityShip, pos: BlockPos) = {

    val blockstate = ship.Shipworld.getBlockState(pos)

    val block = blockstate.getBlock

    block.setBlockBoundsBasedOnState(ship.Shipworld, pos)

    val aabb = block.getSelectedBoundingBox(ship.Shipworld, pos)

    val rotatedBB = new RotatedBB(aabb, new Vec3(0.5, 0.5, 0.5), new Quat4f(0, 0, 0, 1))

    val tessellator: Tessellator = Tessellator.getInstance
    val worldrenderer: WorldRenderer = tessellator.getWorldRenderer
    worldrenderer.setTranslation(0, 0, 0)

    GlStateManager.enableBlend()
    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
    GlStateManager.color(0.0F, 0.0F, 0.0F, 0.4F)
    GL11.glLineWidth(2.0F)
    GlStateManager.disableTexture2D()
    GlStateManager.depthMask(false)

    RenderUtils.drawRotatedBB(rotatedBB.expand(0.002), worldrenderer)

    GlStateManager.depthMask(true)
    GlStateManager.enableTexture2D()
    GlStateManager.disableBlend()


  }

  // Draws things useful for debugging
  private def doDebugRender(shipWorld: ShipWorldClient) = {
    DebugRender.drawRotatedBoundingBox(new RotatedBB(shipWorld.Ship.getBoundingBox.RelativeAABB, new Vec3(0.5, 0.5, 0.5), shipWorld.Ship.getInverseRotation), shipWorld.Ship)

    shipWorld.BlocksOnShip.foreach(uPos => {
      val blockState = shipWorld.getBlockState(uPos.RelativePos)
      DebugRender.debugRenderBlock(shipWorld, blockState, uPos.RelativePos)

    })
  }

  // Converts a Matrix4f to a FloatBuffer so we can pass it to OpenGL
  // Used for Ship Rotation
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

  // Converts a Quaternion to a Matrix4f
  // Used for Ship Rotation
  private def quaternionToMatrix4f(q: Quat4f): Matrix4f = {
    q.normalize()
    new Matrix4f(1.0f - 2.0f * q.getY * q.getY - 2.0f * q.getZ * q.getZ, 2.0f * q.getX * q.getY - 2.0f * q.getZ * q.getW, 2.0f * q.getX * q.getZ + 2.0f * q.getY * q.getW, 0.0f,
      2.0f * q.getX * q.getY + 2.0f * q.getZ * q.getW, 1.0f - 2.0f * q.getX * q.getX - 2.0f * q.getZ * q.getZ, 2.0f * q.getY * q.getZ - 2.0f * q.getX * q.getW, 0.0f,
      2.0f * q.getX * q.getZ - 2.0f * q.getY * q.getW, 2.0f * q.getY * q.getZ + 2.0f * q.getX * q.getW, 1.0f - 2.0f * q.getX * q.getX - 2.0f * q.getY * q.getY, 0.0f,
      0.0f, 0.0f, 0.0f, 1.0f)
  }


}
