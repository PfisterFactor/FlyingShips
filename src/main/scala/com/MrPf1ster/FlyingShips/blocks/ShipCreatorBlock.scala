package com.MrPf1ster.FlyingShips.blocks

import com.MrPf1ster.FlyingShips.entities.ShipEntity
import com.MrPf1ster.FlyingShips.util.BlockUtils
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{BlockPos, EnumFacing}
import net.minecraft.world.World
import net.minecraftforge.fml.common.registry.GameRegistry

class ShipCreatorBlock extends Block(Material.wood) {
  def name = "shipCreatorBlock"

  GameRegistry.registerBlock(this, name)
  setUnlocalizedName("flyingships" + "_" + name)
  setCreativeTab(CreativeTabs.tabBlock)
  setHardness(2.0f)

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





}
