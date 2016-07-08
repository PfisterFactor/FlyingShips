package com.MrPf1ster.FlyingShips.world

import net.minecraft.profiler.Profiler
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockPos
import net.minecraft.world.biome.BiomeGenBase
import net.minecraft.world.border.WorldBorder
import net.minecraft.world.chunk.IChunkProvider
import net.minecraft.world.{EnumSkyBlock, World}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}



class DetachedWorld(OriginWorld:World, WorldName:String)
  extends World(SaveHandler, OriginWorld.getWorldInfo, OriginWorld.provider, new Profiler(), OriginWorld.isRemote) {

  //worldAccesses = OriginWorld.worldAccesses

  override def getRenderDistanceChunks: Int = 0

  override def createChunkProvider(): IChunkProvider = null

  @SideOnly(Side.CLIENT)
  override def getLightBrightness(pos: BlockPos) = 15

  override def getBiomeGenForCoords(pos: BlockPos): BiomeGenBase = null

  override def getBiomeGenForCoordsBody(pos: BlockPos): BiomeGenBase = null
  override def isChunkLoaded(x:Int, z:Int, something:Boolean) = true
  override def getChunkFromChunkCoords(x:Int,z:Int) = null
  override def getProviderName = "Detached World"
  override def isBlockFullCube(pos:BlockPos) = false

  override def isBlockLoaded(pos: BlockPos) = true

  override def getWorldBorder = new WorldBorder()

  override def getLightFromNeighbors(pos: BlockPos) = 15

  override def getLightFromNeighborsFor(typ: EnumSkyBlock, pos: BlockPos) = {
    if (typ == EnumSkyBlock.SKY)
      15
    else
      4
  }

  override def markChunkDirty(blockPos: BlockPos, unused: TileEntity) = {}




}
