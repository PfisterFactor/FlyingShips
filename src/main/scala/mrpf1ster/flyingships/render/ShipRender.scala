package mrpf1ster.flyingships.render

import java.nio.FloatBuffer
import javax.vecmath.{Matrix4f, Quat4f}

import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.util.{RenderUtils, RotatedBB, UnifiedPos}
import mrpf1ster.flyingships.world.{ShipWorld, ShipWorldClient}
import net.minecraft.block._
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer._
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
object ShipRender {
  private val texturemap = Minecraft.getMinecraft.getTextureMapBlocks
  val destroyBlockIcons: Array[TextureAtlasSprite] = Array.range(0, 10).map(i => texturemap.getAtlasSprite(s"minecraft:blocks/destroy_stage_$i"))
}

@SideOnly(Side.CLIENT)
class ShipRender(rm: RenderManager) extends Render[EntityShip](rm) {
  var displayListIDs: mMap[ShipWorld, Int] = mMap()

  // Ships are dynamic, and thus don't have a texture
  override def getEntityTexture(entity: EntityShip): ResourceLocation = null

  // No shadow rendering
  override def doRenderShadowAndFire(entity: Entity, x: Double, y: Double, z: Double, yaw: Float, partialTickTime: Float) = {}


  override def doRender(entity: EntityShip, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) = {

    def shipWorld = entity.Shipworld.asInstanceOf[ShipWorldClient]



    GL11.glPushMatrix()

    // Translate away from player
    GL11.glTranslated(x, y, z)


    // Translate into the center of the ship block for rotation
    GL11.glTranslated(0.5, 0.5, 0.5)

    // Interpolate rotation so it doesn't look jerky
    entity.oldRotation.interpolate(entity.getRotation, 1.0f)

    // Turn a quaternion into a matrix, then into a FloatBuffer
    val rotationBuffer = matrixToFloatBuffer(quaternionToMatrix4f(entity.oldRotation)) //entity.renderMatrix)


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
    shipWorld.loadedTileEntityList
      .filter(te => TileEntityRendererDispatcher.instance.getSpecialRenderer(te) != null)
      .foreach(te => {
        val relPos: BlockPos = te.getPos.subtract(ShipWorld.ShipBlockPos) // From this we get relative distance from ship block, (ship block = x:0,y:0,z:0)
        val worldPos: BlockPos = UnifiedPos.convertToWorld(te.getPos, shipWorld.OriginPos())
        te.setWorldObj(shipWorld)
        // If anyone ever wants to explain why the below works i'll be happy to hear
        TileEntityRendererDispatcher.instance.renderTileEntityAt(te, -(worldPos.getX - 2 * relPos.getX), -(worldPos.getY - 2 * relPos.getY), -(worldPos.getZ - 2 * relPos.getZ), partialTicks, -1)
      })

    // Block breaking textures
    renderBlockBreaking(shipWorld)


    GL11.glPopMatrix()
    if (DebugRender.isDebugMenuShown)
      doDebugRender(shipWorld, x, y, z)

    // Ray-trace across the ship and return it wrapped in an option
    val rayTrace: Option[MovingObjectPosition] = entity.InteractionHandler.getBlockPlayerIsLookingAt(partialTicks)

    if (rayTrace.isDefined) {
      //println(hoveredBlockOnShip.get)
      def block = rayTrace.get.getBlockPos
      renderBlackOutline(entity, block, x, y, z, partialTicks)
    }

    RenderHelper.enableStandardItemLighting()


  }

  private def getDisplayList(shipWorld: ShipWorldClient): Int = {
    var id = displayListIDs.get(shipWorld)
    if (id.isDefined && shipWorld.doRenderUpdate) {
      GL11.glDeleteLists(id.get, 1)
      id = None
      shipWorld.doRenderUpdate = false
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

  private def renderShip(shipWorld: ShipWorldClient) = {
    // Setup tessellator and worldRenderer
    def tessellator = Tessellator.getInstance()
    def worldRenderer = tessellator.getWorldRenderer

    val i: Double = shipWorld.Ship.posX + ShipWorld.ShipBlockPos.getX
    val j: Double = shipWorld.Ship.posY + ShipWorld.ShipBlockPos.getY
    val k: Double = shipWorld.Ship.posZ + ShipWorld.ShipBlockPos.getZ
    worldRenderer.setTranslation(-i, -j, -k)
    worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK)

    shipWorld.BlocksOnShip.foreach(uPos => {
      val blockState = shipWorld.getBlockState(uPos.RelativePos)
      renderBlock(shipWorld, blockState, uPos.RelativePos, worldRenderer)
    })
    /*
    shipWorld.BlockSet.foreach(uPos => {
      val blockState = shipWorld.getBlockState(uPos.RelativePos)
      renderBlock(shipWorld, blockState, uPos.RelativePos, worldRenderer)
    })
    */

    worldRenderer.setTranslation(0, 0, 0)
    tessellator.draw()


  }

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
        val textureatlassprite: TextureAtlasSprite = ShipRender.destroyBlockIcons(i)
        val blockrendererdispatcher: BlockRendererDispatcher = Minecraft.getMinecraft.getBlockRendererDispatcher
        blockrendererdispatcher.renderBlockDamage(iblockstate, blockpos, textureatlassprite, shipWorldClient)
      }
    }
  }

  def renderBlockBreaking(shipWorld: ShipWorldClient): Unit = {
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
    worldRenderer.setTranslation(-vec.xCoord, -vec.yCoord, -vec.zCoord)
    worldRenderer.noColor()
    shipWorld.ShipRenderGlobal.DamagedBlocks.foreach(pair => renderBlockBreak(pair._2, shipWorld))
    worldRenderer.setTranslation(0.0D, 0.0D, 0.0D)
    tessellator.draw()


    GlStateManager.disableAlpha()
    GlStateManager.doPolygonOffset(0.0F, 0.0F)
    GlStateManager.disablePolygonOffset()
    GlStateManager.enableAlpha()
    GlStateManager.depthMask(true)
  }

  def renderBlock(shipWorld: ShipWorld, blockState: IBlockState, pos: BlockPos, worldRenderer: WorldRenderer) = {
    // Get the block renderer
    def blockRendererDispatcher = Minecraft.getMinecraft.getBlockRendererDispatcher

    // Renders blocks based on their render type
    blockRendererDispatcher.renderBlock(blockState, pos, shipWorld, worldRenderer)

  }

  private def renderBlackOutline(ship: EntityShip, pos: BlockPos, x: Double, y: Double, z: Double, partialTicks: Float) = {

    val blockstate = ship.Shipworld.getBlockState(pos)

    val block = blockstate.getBlock

    block.setBlockBoundsBasedOnState(ship.Shipworld, pos)

    val aabb = block.getSelectedBoundingBox(ship.Shipworld, pos)

    val rotatedBB = new RotatedBB(aabb, new Vec3(0.5, 0.5, 0.5), ship.getRotation)

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
    worldrenderer.setTranslation(-ship.posX - ShipWorld.ShipBlockPos.getX - pos.getX, -ship.posY - ShipWorld.ShipBlockPos.getY - pos.getY, -ship.posZ - ShipWorld.ShipBlockPos.getZ - pos.getZ)

    RenderUtils.drawRotatedBB(rotatedBB.expand(0.001), worldrenderer)

    worldrenderer.setTranslation(0, 0, 0)

    GlStateManager.depthMask(true)
    GlStateManager.enableTexture2D()
    GlStateManager.disableBlend()

    GL11.glPopMatrix()

  }


  private def doDebugRender(shipWorld: ShipWorldClient, x: Double, y: Double, z: Double) = {
    DebugRender.drawRotatedBoundingBox(new RotatedBB(shipWorld.Ship.getBoundingBox.RelativeAABB, new Vec3(0, 0, 0), new Quat4f(0, 0, 0, 1)), shipWorld.Ship, x, y, z)

    shipWorld.BlocksOnShip.foreach(uPos => {
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
