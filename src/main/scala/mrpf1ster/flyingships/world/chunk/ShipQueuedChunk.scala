package mrpf1ster.flyingships.world.chunk

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraft.world.chunk.storage.AnvilChunkLoader

/**
  * Created by EJ on 7/20/2016.
  */
case class ShipQueuedChunk(x: Int, z: Int, loader: AnvilChunkLoader, world: World, provider: ChunkProviderShip) {
  var compound: NBTTagCompound = null
  override def hashCode: Int = {
    (x * 31 + z * 29) ^ world.hashCode
  }

  override def equals(other: Any): Boolean = other match {
      case other1: ShipQueuedChunk => x == other1.x && z == other1.z && world == other1.world
      case _ => false
  }

  override def toString: String = {
    val result: StringBuilder = new StringBuilder
    val NEW_LINE: String = System.getProperty("line.separator")
    result.append(this.getClass.getName + " {" + NEW_LINE)
    result.append(" x: " + x + NEW_LINE)
    result.append(" z: " + z + NEW_LINE)
    result.append(" loader: " + loader + NEW_LINE)
    result.append(" world: " + world.getWorldInfo.getWorldName + NEW_LINE)
    result.append(" dimension: " + world.provider.getDimensionId + NEW_LINE)
    result.append(" provider: " + world.provider.getClass.getName + NEW_LINE)
    result.append("}")
    result.toString
  }
}
