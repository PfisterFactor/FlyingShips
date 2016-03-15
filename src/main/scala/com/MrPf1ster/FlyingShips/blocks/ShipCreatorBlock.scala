package com.MrPf1ster.FlyingShips.blocks

import com.MrPf1ster.FlyingShips.entities.ShipEntity
import com.MrPf1ster.FlyingShips.util.BlockUtils
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.properties.{IProperty, PropertyDirection}
import net.minecraft.block.state.{BlockState, IBlockState}
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{BlockPos, EnumFacing}
import net.minecraft.world.World
import net.minecraftforge.fml.common.registry.GameRegistry

object ShipCreatorBlock {
  val FACING: PropertyDirection = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL)
}
class ShipCreatorBlock extends Block(Material.wood) {
  def name = "shipCreatorBlock"

  GameRegistry.registerBlock(this, name)
  setUnlocalizedName("flyingships" + "_" + name)
  setCreativeTab(CreativeTabs.tabBlock)
  setHardness(2.0f)
  setDefaultState(blockState.getBaseState.withProperty(ShipCreatorBlock.FACING, EnumFacing.NORTH))

  // TODO: Up the limit from 100 and figure out why it crashes
  override def onBlockActivated(worldIn:World, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer,side: EnumFacing,hitX: Float, hitY: Float, hitZ: Float) : Boolean = {
    val blocksConnected = BlockUtils.findAllBlocksConnected(worldIn, pos)
    if (blocksConnected.size <= 100) {
      val shipEntity = new ShipEntity(pos, worldIn, blocksConnected, pos)
      shipEntity.setPosition(pos.getX, pos.getY, pos.getZ)

      worldIn.spawnEntityInWorld(shipEntity)

      // Destroy all the blocks

      blocksConnected.foreach(pos => {
        worldIn.removeTileEntity(pos)
        worldIn.setBlockToAir(pos)
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

  override def onBlockPlaced(world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, meta: Int, placer: EntityLivingBase) =
    getDefaultState.withProperty(ShipCreatorBlock.FACING, placer.getHorizontalFacing)






}
