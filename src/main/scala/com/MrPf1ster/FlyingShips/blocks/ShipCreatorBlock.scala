package com.MrPf1ster.FlyingShips.blocks

import com.MrPf1ster.FlyingShips.FlyingShips
import com.MrPf1ster.FlyingShips.entities.ShipEntity
import com.MrPf1ster.FlyingShips.util.BlockUtils

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.util.{EnumFacing, BlockPos}
import net.minecraft.world.World
import net.minecraftforge.fml.common.registry.GameRegistry

class ShipCreatorBlock extends Block(Material.wood) {
  def name = "shipCreatorBlock"

  GameRegistry.registerBlock(this, name)
  setUnlocalizedName("flyingships" + "_" + name)
  setCreativeTab(CreativeTabs.tabBlock)
  setHardness(2.0f)


  def time[R](block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block    // call-by-name
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
    result
  }

  override def onBlockActivated(worldIn:World, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer,side: EnumFacing,hitX: Float, hitY: Float, hitZ: Float) : Boolean = {
    val mySet = time { BlockUtils.findAllBlocksConnected(worldIn,pos) }

    val s = new ShipEntity(pos,worldIn,mySet,pos,playerIn)
    s.setPosition(pos.getX,pos.getY,pos.getZ)

    worldIn.spawnEntityInWorld(s)

    true
  }





}
