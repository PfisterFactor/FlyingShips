package mrpf1ster.flyingships.world

import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.render.ShipRenderGlobal
import mrpf1ster.flyingships.util.UnifiedPos
import mrpf1ster.flyingships.world.chunk.ClientChunkProviderShip
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.world.World
import net.minecraft.world.chunk.{Chunk, IChunkProvider}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by ej on 7/25/16.
  */
@SideOnly(Side.CLIENT)
class ShipWorldClient(originWorld: World, ship: EntityShip) extends ShipWorld(originWorld, ship) {
  var doRenderUpdate = false
  addWorldAccess(new ShipRenderGlobal(this))

  override def createChunkProvider(): IChunkProvider = new ClientChunkProviderShip(this)

  override def applyBlockChange(pos: BlockPos, newState: IBlockState, flags: Int): Boolean = {
    val uPos = UnifiedPos(pos, OriginPos, IsRelative = true)
    val contains = BlocksOnShip.contains(uPos)
    BlocksOnShip.add(uPos)
    if (!contains && newState.getBlock != Blocks.air) {
      Ship.generateBoundingBox()
    }
    else if (newState.getBlock == Blocks.air) {
      BlocksOnShip.remove(uPos)
      Ship.generateBoundingBox()
    }

    val chunk: Chunk = this.getChunkFromBlockCoords(pos)
    val block: Block = newState.getBlock
    val oldBlock: Block = getBlockState(pos).getBlock
    val oldLight: Int = oldBlock.getLightValue(this, pos)
    val oldOpacity: Int = oldBlock.getLightOpacity(this, pos)
    val iblockstate: IBlockState = chunk.setBlockState(pos, newState)

    if (iblockstate == null) return false
    if (block.getLightOpacity(this, pos) != oldOpacity || block.getLightValue(this, pos) != oldLight)
      this.checkLight(pos)


    this.markAndNotifyBlock(pos, chunk, iblockstate, newState, flags)

    doRenderUpdate = true
    true
  }

  override def tick() = {}

  override def setBlockState(pos: BlockPos, newState: IBlockState, flags: Int) = true

  override def onShipMove() = {
    doRenderUpdate = true
  }

  override def addBlockEvent(pos: BlockPos, block: Block, eventID: Int, eventParam: Int) = block.onBlockEventReceived(this, pos, getBlockState(pos), eventID, eventParam)

}
