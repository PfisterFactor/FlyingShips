package com.MrPf1ster.FlyingShips.world

import java.io.File

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.WorldProvider
import net.minecraft.world.chunk.storage.IChunkLoader
import net.minecraft.world.storage.{IPlayerFileData, ISaveHandler, WorldInfo}

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
