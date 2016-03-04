package com.MrPf1ster.FlyingShips.render

import com.MrPf1ster.FlyingShips.ShipWorld
import com.MrPf1ster.FlyingShips.entities.ShipEntity
import net.minecraft.block.Block
import net.minecraft.block.state.{IBlockState, BlockState}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.client.renderer.entity.{RenderManager, Render}
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer._
import net.minecraft.util.{BlockPos, ResourceLocation}
import net.minecraft.entity.Entity
import net.minecraft.world.IBlockAccess
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
    println("render being called")
    GL11.glPushMatrix()
    GL11.glTranslated( x, y, z )
    GL11.glRotatef(entityYaw, 0.0f, 1.0f, 0.0f)
    GL11.glTranslated( entity.ShipBlockPos.getX, entity.ShipBlockPos.getY, entity.ShipBlockPos.getZ)
    RenderHelper.disableStandardItemLighting()
    rm.worldObj = shipWorld
    rm.renderEngine.bindTexture(TextureMap.locationBlocksTexture )
    GL11.glCallList(getDisplayList(shipWorld))
    //renderShip(shipWorld)



    RenderHelper.enableStandardItemLighting()
    GL11.glPopMatrix()


  }
  def getDisplayList(shipworld:ShipWorld): Int = {
    var id = displayListIDs.get(shipworld)
    if (id != None && shipworld.needsRenderUpdate) {
      GL11.glDeleteLists(id.get,1)
      id = null
      println("Delete List")
    }
    if (id == None) {
      println("New List")
      // create a new list
      id = Option[Int](GLAllocation.generateDisplayLists( 1 ))
      displayListIDs.put(shipworld,id.get)

      // build the list
      GL11.glNewList( id.get, GL11.GL_COMPILE )
      renderShip(shipworld )
      GL11.glEndList()
    }
    return id.get
  }



  def renderShip(shipWorld: ShipWorld) = {
    def tessellator = Tessellator.getInstance()
    def worldRenderer = tessellator.getWorldRenderer
    val i: Int = shipWorld.OriginPos.getX
    val j: Int = shipWorld.OriginPos.getY
    val k: Int = shipWorld.OriginPos.getZ
    worldRenderer.setTranslation(-i,-j,-k)
    worldRenderer.begin(7,DefaultVertexFormats.BLOCK)

    println(shipWorld.BlockSet.size)
    shipWorld.BlockSet.foreach(blockPos => {
      val blockState = shipWorld.getBlockState(blockPos)
      renderBlock(shipWorld,blockState,blockPos,worldRenderer)
    })
    worldRenderer.setTranslation(0,0,0)
    tessellator.draw()
  }
  def renderBlock(shipWorld: ShipWorld, blockState: IBlockState,pos:BlockPos,worldRenderer: WorldRenderer) = {
    def blockRendererDispatcher = Minecraft.getMinecraft.getBlockRendererDispatcher

    val bakedModel = blockRendererDispatcher.getModelFromBlockState(blockState,shipWorld,pos)
    val te = null//shipWorld.getTileEntity(pos)
    if (te == null || true) { //TileEntityRendererDispatcher.instance.getSpecialRenderer(te) == null) {
      if (blockState.getBlock.getRenderType == 3)
        println("we got here, so thats good")
        blockRendererDispatcher.getBlockModelRenderer.renderModel(shipWorld,bakedModel,blockState,pos,worldRenderer)
    }

  }
}
