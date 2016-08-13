package mrpf1ster.flyingships.blocks

import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.entities.EntityShip
import mrpf1ster.flyingships.util.{BlockUtils, UnifiedPos}
import mrpf1ster.flyingships.world.ShipWorld
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.properties.{IProperty, PropertyDirection}
import net.minecraft.block.state.{BlockState, IBlockState}
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.{BlockPos, EnumFacing}
import net.minecraft.world.World
import net.minecraftforge.fml.common.registry.GameRegistry

object ShipCreatorBlock {
  val FACING: PropertyDirection = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL)
}
class ShipCreatorBlock extends Block(Material.wood) {
  final val name = "shipcreatorblock"
  setRegistryName(FlyingShips.MOD_ID,name)
  setUnlocalizedName(getRegistryName)
  setCreativeTab(CreativeTabs.tabBlock)
  setHardness(2.0f)
  setDefaultState(blockState.getBaseState.withProperty(ShipCreatorBlock.FACING, EnumFacing.NORTH))
  GameRegistry.registerBlock(this, name)



  // TODO: Up the limit from 100 and figure out why it crashes
  override def onBlockActivated(worldIn:World, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer,side: EnumFacing,hitX: Float, hitY: Float, hitZ: Float) : Boolean = {

    if (worldIn.isInstanceOf[ShipWorld]) return true
    if (worldIn.isRemote) return true

    val blocksConnected = BlockUtils.findAllBlocksConnected(worldIn, pos)
    if (blocksConnected.size > 1) {
      val shipEntity = new EntityShip(pos, worldIn)
      shipEntity.setLocationAndAngles(pos.getX,pos.getY,pos.getZ,0,0)
      shipEntity.createShipWorld()
      shipEntity.Shipworld.moveBlocks(blocksConnected.map(UnifiedPos(_, shipEntity.Shipworld.OriginPos, IsRelative = false)))
      shipEntity.setShipID(EntityShip.getNextShipID())

      EntityShip.addShipToWorld(shipEntity)

      // Destroy all the blocks
      blocksConnected.foreach(pos => {
        worldIn.removeTileEntity(pos)
        worldIn.setBlockState(pos, Blocks.air.getDefaultState, 2)
      })
    }
    true
  }

  override def getStateFromMeta(meta: Int) = {
    var facing = EnumFacing.getFront(meta)

    if (facing.getAxis == EnumFacing.Axis.Y) {
      facing = EnumFacing.NORTH
    }

    getDefaultState.withProperty(ShipCreatorBlock.FACING, facing)
  }

  override def createBlockState(): BlockState = {
    new BlockState(this, ShipCreatorBlock.FACING.asInstanceOf[IProperty[Nothing]])
  }

  override def getMetaFromState(state: IBlockState) = {
    val a = state.getValue(ShipCreatorBlock.FACING)
    a.getHorizontalIndex
  }

  override def onBlockPlaced(world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, meta: Int, placer: EntityLivingBase):IBlockState =
    getDefaultState.withProperty(ShipCreatorBlock.FACING, placer.getHorizontalFacing)


}
