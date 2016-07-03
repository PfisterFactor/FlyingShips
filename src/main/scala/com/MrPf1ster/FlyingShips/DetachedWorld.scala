package com.MrPf1ster.FlyingShips

import java.io.File

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.profiler.Profiler
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockPos
import net.minecraft.world.biome.BiomeGenBase
import net.minecraft.world.chunk.IChunkProvider
import net.minecraft.world.chunk.storage.IChunkLoader
import net.minecraft.world.storage.{IPlayerFileData, ISaveHandler, WorldInfo}
import net.minecraft.world.{EnumSkyBlock, World, WorldProvider}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by EJ on 3/2/2016.
  */

object SaveHandler extends ISaveHandler {
  override def getWorldDirectory: File = null

  override def getWorldDirectoryName: String = null

  override def checkSessionLock(): Unit = {}

  override def saveWorldInfoWithPlayer(worldInformation: WorldInfo, tagCompound: NBTTagCompound): Unit = {}

  override def flush(): Unit = {}

  override def loadWorldInfo(): WorldInfo = null

  override def getChunkLoader(provider: WorldProvider): IChunkLoader = null

  override def getMapFileFromName(mapName: String): File = null

  override def getPlayerNBTManager: IPlayerFileData = null

  override def saveWorldInfo(worldInformation: WorldInfo): Unit = {}
}

class DetachedWorld(OriginWorld:World, WorldName:String)
  extends World(SaveHandler, OriginWorld.getWorldInfo, OriginWorld.provider, new Profiler(), OriginWorld.isRemote) {


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


  override def getLightFromNeighbors(pos: BlockPos) = 15

  override def getLightFromNeighborsFor(typ: EnumSkyBlock, pos: BlockPos) = {
    if (typ == EnumSkyBlock.SKY)
      15
    else
      4
  }

  override def markChunkDirty(blockPos: BlockPos, unused: TileEntity) = {}




}
