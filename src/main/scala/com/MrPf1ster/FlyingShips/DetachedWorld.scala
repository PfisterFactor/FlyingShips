package com.MrPf1ster.FlyingShips

import java.io.File
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.profiler.Profiler
import net.minecraft.util.BlockPos
import net.minecraft.world.chunk.IChunkProvider
import net.minecraft.world.chunk.storage.IChunkLoader
import net.minecraft.world.{WorldProvider, World}
import net.minecraft.world.storage.{IPlayerFileData, WorldInfo, ISaveHandler}
import net.minecraftforge.fml.relauncher.Side
import scala.tools.nsc.doc.model.Entity
import net.minecraftforge.fml.relauncher.SideOnly

/**
  * Created by EJ on 3/2/2016.
  */

// Cuchaz's influence on my code is very obvious isn't it :)
object SaveHandler extends ISaveHandler {
  override def getWorldDirectory: File = null

  override def getWorldDirectoryName: String = null

  override def checkSessionLock(): Unit = ???

  override def saveWorldInfoWithPlayer(worldInformation: WorldInfo, tagCompound: NBTTagCompound): Unit = {}

  override def flush(): Unit = {}

  override def loadWorldInfo(): WorldInfo = null

  override def getChunkLoader(provider: WorldProvider): IChunkLoader = null

  override def getMapFileFromName(mapName: String): File = null

  override def getPlayerNBTManager: IPlayerFileData = null

  override def saveWorldInfo(worldInformation: WorldInfo): Unit = {}
}

class DetachedWorld(realWorld:World, worldName:String)
  extends World(SaveHandler, realWorld.getWorldInfo, realWorld.provider, new Profiler(), realWorld.isRemote) {
  override def getRenderDistanceChunks: Int = 1 //TODO: I don't actually know if this needs to be 1

  override def createChunkProvider(): IChunkProvider = null



  @SideOnly(Side.CLIENT)
  override def getLightBrightness(pos:BlockPos) = 0

  override def getBiomeGenForCoords(pos:BlockPos) = null
  override def getBiomeGenForCoordsBody(pos:BlockPos) = null
  override def isChunkLoaded(x:Int, z:Int, something:Boolean) = true
  override def getChunkFromChunkCoords(x:Int,z:Int) = null
  override def getProviderName = "Detached World"
  override def isBlockFullCube(pos:BlockPos) = false
}
