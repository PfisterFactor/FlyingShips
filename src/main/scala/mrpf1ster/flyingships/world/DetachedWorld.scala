package mrpf1ster.flyingships.world

import java.io.File
import java.util.UUID

import net.minecraft.profiler.Profiler
import net.minecraft.util.BlockPos
import net.minecraft.world.biome.BiomeGenBase
import net.minecraft.world.border.WorldBorder
import net.minecraft.world.chunk.IChunkProvider
import net.minecraft.world.chunk.storage.AnvilSaveHandler
import net.minecraft.world.{EnumSkyBlock, World}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}


class DetachedWorld(OriginWorld: World, WorldName: String, uuid: UUID)
  extends World(new AnvilSaveHandler(new File(OriginWorld.getSaveHandler.getWorldDirectory, "ShipWorlds"), s"$uuid", false), OriginWorld.getWorldInfo, OriginWorld.provider, new Profiler(), OriginWorld.isRemote) {
  val UUID = uuid
  //worldAccesses = OriginWorld.worldAccesses

  override def getRenderDistanceChunks: Int = 0

  override def createChunkProvider(): IChunkProvider = null

  @SideOnly(Side.CLIENT)
  override def getLightBrightness(pos: BlockPos) = 15

  override def getBiomeGenForCoords(pos: BlockPos): BiomeGenBase = null

  override def getBiomeGenForCoordsBody(pos: BlockPos): BiomeGenBase = null
  override def getProviderName = "Detached World"

  override def getWorldBorder = new WorldBorder()

  override def getLightFromNeighbors(pos: BlockPos) = 15

  override def getLightFromNeighborsFor(typ: EnumSkyBlock, pos: BlockPos) = {
    if (typ == EnumSkyBlock.SKY)
      15
    else
      4
  }


}
