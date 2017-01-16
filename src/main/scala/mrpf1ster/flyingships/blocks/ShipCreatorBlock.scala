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

// Holds a property we have aptly called FACING
// The capitalization scares the meek
object ShipCreatorBlock {
  val FACING: PropertyDirection = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL)
}

// The (terribly textured) ShipCreatorBlock™!
// When you right click it, you make a ship with the blocks connected to it
// Well that's the hope anyways
class ShipCreatorBlock extends Block(Material.wood) {
  final val name = "shipcreatorblock"
  // Some boilerplate
  setRegistryName(FlyingShips.MOD_ID,name)
  setUnlocalizedName(getRegistryName)
  setCreativeTab(CreativeTabs.tabBlock)
  setHardness(2.0f)
  setDefaultState(blockState.getBaseState.withProperty(ShipCreatorBlock.FACING, EnumFacing.NORTH))
  GameRegistry.registerBlock(this, name)


  // Called every time someone right clicks it
  // We ignore it on client
  override def onBlockActivated(worldIn:World, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer,side: EnumFacing,hitX: Float, hitY: Float, hitZ: Float) : Boolean = {

    if (worldIn.isRemote) return true
    if (worldIn.isInstanceOf[ShipWorld]) return true

    // Get all the blocks connected to the ship block
    val blocksConnected = BlockUtils.findAllBlocksConnected(worldIn, pos)
    // If there's more blocks than just itself
    if (blocksConnected.size > 1) {
      // Create a ship
      val shipEntity = new EntityShip(pos, worldIn)
      // Setup said ship
      shipEntity.setLocationAndAngles(pos.getX,pos.getY,pos.getZ,0,0)
      shipEntity.createShipWorld()
      // Move blocks onto the ship
      shipEntity.Shipworld.moveBlocks(blocksConnected.map(UnifiedPos(_, shipEntity.Shipworld.OriginPos, IsRelative = false)))
      // Give it a nice name for its creation
      // I'm thinking: "Ship ID: 68391" rings a nice bell doesn't it?
      shipEntity.setShipID(EntityShip.getNewShipID())

      // Add it to our entity tracker
      // Forge's entity tracker doesn't like big entities
      EntityShip.addShipToWorld(shipEntity)

      // Destroy all the blocks
      // Todo: Do this without dropping blocks
      blocksConnected.foreach(pos => {
        worldIn.removeTileEntity(pos)
        worldIn.setBlockState(pos, Blocks.air.getDefaultState, 2)
      })
    }
    true
  }

  // Returns a block state from meta
  override def getStateFromMeta(meta: Int) = {
    var facing = EnumFacing.getFront(meta)

    if (facing.getAxis == EnumFacing.Axis.Y) {
      facing = EnumFacing.NORTH
    }

    getDefaultState.withProperty(ShipCreatorBlock.FACING, facing)
  }

  // Creates a block state
  override def createBlockState(): BlockState = {
    new BlockState(this, ShipCreatorBlock.FACING.asInstanceOf[IProperty[Nothing]])
  }

  // Returns a meta from block state
  override def getMetaFromState(state: IBlockState) = {
    val a = state.getValue(ShipCreatorBlock.FACING)
    a.getHorizontalIndex
  }

  // Called whenever the block is placed
  // Just orients the block to where the placer is facing
  override def onBlockPlaced(world: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, meta: Int, placer: EntityLivingBase):IBlockState =
    getDefaultState.withProperty(ShipCreatorBlock.FACING, placer.getHorizontalFacing)


}
