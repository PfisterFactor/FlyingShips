package mrpf1ster.flyingships.entities

import mrpf1ster.flyingships.util.UnifiedPos
import mrpf1ster.flyingships.world.ShipWorld
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{ItemBlock, ItemStack, ItemSword}
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util._
import net.minecraft.world.{World, WorldSettings}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by EJ on 7/3/2016.
  */
object ClickSimulator {
  var leftClickCounter = 0
  var rightClickDelayTimer = 0
}

// This class is pretty much a carbon copy of PlayerControllerMP
class ClickSimulator(shipWorld: ShipWorld) {


  private var currentBlock = new BlockPos(-1, -1, -1)
  private var currentItemHittingBlock: ItemStack = null
  private var curBlockDamageMP: Float = 0
  private var blockHitDelay = 5
  private var isHittingBlock: Boolean = false

  private def currentGameType: WorldSettings.GameType = Minecraft.getMinecraft.playerController.getCurrentGameType

  private var stepSoundTickCounter: Float = 0

  @SideOnly(Side.CLIENT)
  def rightClickMouse(): Unit = {
    if (!isHittingBlock) {
      var flag: Boolean = true
      val itemstack: ItemStack = Minecraft.getMinecraft.thePlayer.inventory.getCurrentItem
      def player = Minecraft.getMinecraft.thePlayer

      val mop = shipWorld.Ship.InteractionHandler.getBlockPlayerIsLookingAt(1.0f)

      if (mop.isEmpty) return

      ClickSimulator.rightClickDelayTimer = 4
      val blockpos: BlockPos = mop.get.getBlockPos

      if (!shipWorld.isAirBlock(blockpos)) {
        val i: Int = if (itemstack != null) itemstack.stackSize else 0

        val result: Boolean = !net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(player, net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, shipWorld, blockpos, mop.get.sideHit).isCanceled
        if (result) {
          if (onPlayerRightClick(player, blockpos, mop.get.hitVec, mop.get.sideHit)) {
            flag = false
            player.swingItem()
          }
        }
        if (itemstack == null) {
          return
        }
        if (itemstack.stackSize == 0) {
          player.inventory.mainInventory(player.inventory.currentItem) = null
        }
        else if (itemstack.stackSize != i || player.capabilities.isCreativeMode) {
          Minecraft.getMinecraft.entityRenderer.itemRenderer.resetEquippedProgress()
        }
      }

      if (flag) {
        val itemstack1: ItemStack = player.inventory.getCurrentItem
        val result: Boolean = !net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(player, net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_AIR, Minecraft.getMinecraft.theWorld, null, null).isCanceled
        if (result && itemstack1 != null && sendUseItem(player, shipWorld, itemstack1)) {
          Minecraft.getMinecraft.entityRenderer.itemRenderer.resetEquippedProgress2()
        }
      }
    }
  }

  @SideOnly(Side.CLIENT)
  def onPlayerRightClick(player: EntityPlayer, pos: BlockPos, hitVec: Vec3, side: EnumFacing): Boolean = {

    def hitVecX = hitVec.xCoord.toFloat
    def hitVecY = hitVec.yCoord.toFloat
    def hitVecZ = hitVec.zCoord.toFloat

    def heldStack = player.inventory.getCurrentItem

    val blockState = shipWorld.getBlockState(pos)

    if (heldStack != null &&
      heldStack.getItem != null &&
      heldStack.getItem.onItemUseFirst(heldStack, player, shipWorld, pos, side, hitVecX, hitVecY, hitVecZ)) {
      return true
    }

    var blockWasActivated = false

    if (!player.isSneaking || player.getHeldItem == null || player.getHeldItem.getItem.doesSneakBypassUse(shipWorld, pos, player))
      blockWasActivated = blockState.getBlock.onBlockActivated(shipWorld, pos, blockState, player, side, hitVec.xCoord.toFloat, hitVec.yCoord.toFloat, hitVec.zCoord.toFloat)

    if (!blockWasActivated && heldStack != null && heldStack.getItem.isInstanceOf[ItemBlock]) {

      val itemblock: ItemBlock = heldStack.getItem.asInstanceOf[ItemBlock]

      if (!itemblock.canPlaceBlockOnSide(shipWorld, pos, side, player, heldStack))
        return false
    }

    shipWorld.Ship.InteractionHandler.sendBlockPlacedMessage(pos, side, heldStack, hitVec)

    if (blockWasActivated && !player.isSpectator) return true

    if (heldStack == null) return false

    // If the player is in creative, use the item without damaging it or lowering its stacksize
    if (player.capabilities.isCreativeMode) {
      val meta: Int = heldStack.getMetadata
      val stackSize: Int = heldStack.stackSize
      val itemWasUsed: Boolean = heldStack.onItemUse(player, shipWorld, pos, side, hitVecX, hitVecY, hitVecZ)
      heldStack.setItemDamage(meta)
      heldStack.stackSize = stackSize
      return itemWasUsed
    }

    val itemWasUsed = heldStack.onItemUse(player, shipWorld, pos, side, hitVecX, hitVecY, hitVecZ)

    if (!itemWasUsed)
      return false

    if (heldStack.stackSize <= 0) {
      net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, heldStack)
      player.destroyCurrentEquippedItem()
    }

    true

  }

  def sendUseItem(playerIn: EntityPlayer, worldIn: World, itemStackIn: ItemStack): Boolean = {
    if (this.currentGameType == WorldSettings.GameType.SPECTATOR) {
      false
    }
    else {
      shipWorld.Ship.InteractionHandler.sendBlockPlacedMessage(playerIn.inventory.getCurrentItem)
      val i: Int = itemStackIn.stackSize
      val itemstack: ItemStack = itemStackIn.useItemRightClick(worldIn, playerIn)
      if (itemstack != itemStackIn || itemstack != null && itemstack.stackSize != i) {
        playerIn.inventory.mainInventory(playerIn.inventory.currentItem) = itemstack
        if (itemstack.stackSize <= 0) {
          playerIn.inventory.mainInventory(playerIn.inventory.currentItem) = null
          net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(playerIn, itemstack)
        }
        true
      }
      else {
        false
      }
    }
  }


  @SideOnly(Side.CLIENT)
  def clickBlock(player: EntityPlayer, pos: BlockPos, side: EnumFacing): Boolean = {
    // No Left clicking for spectators
    if (currentGameType == WorldSettings.GameType.SPECTATOR) return false

    if (currentGameType.isAdventure) {

      if (!player.isAllowEdit) {
        val block: Block = shipWorld.getBlockState(pos).getBlock
        val itemstack: ItemStack = player.getCurrentEquippedItem

        if (itemstack == null) return false
        if (!itemstack.canDestroy(block)) return false
      }
    }

    if (!shipWorld.getWorldBorder.contains(pos)) return false

    if (currentGameType.isCreative) {
      shipWorld.Ship.InteractionHandler.sendBlockDiggingMessage(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, side)
      clickBlockCreative(player, pos, side)
      this.blockHitDelay = 5
      return true
    }

    if (!this.isHittingBlock || !isHittingPosition(pos)) {
      if (this.isHittingBlock)
        shipWorld.Ship.InteractionHandler.sendBlockDiggingMessage(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, pos, side)

      shipWorld.Ship.InteractionHandler.sendBlockDiggingMessage(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, side)
      val block1: Block = shipWorld.getBlockState(pos).getBlock
      val blockIsNotAir: Boolean = block1.getMaterial != Material.air

      if (blockIsNotAir && this.curBlockDamageMP == 0.0F)
        block1.onBlockClicked(shipWorld, pos, Minecraft.getMinecraft.thePlayer)

      if (blockIsNotAir && block1.getPlayerRelativeBlockHardness(Minecraft.getMinecraft.thePlayer, shipWorld, pos) >= 1.0F) {
        this.onPlayerDestroyBlock(player, pos, side)
      }
      else {
        this.isHittingBlock = true
        this.currentBlock = pos
        this.currentItemHittingBlock = Minecraft.getMinecraft.thePlayer.getHeldItem
        this.curBlockDamageMP = 0.0F
        this.stepSoundTickCounter = 0.0F
        shipWorld.sendBlockBreakProgress(Minecraft.getMinecraft.thePlayer.getEntityId, this.currentBlock, (this.curBlockDamageMP * 10.0F).toInt - 1)
      }
    }
    true


  }

  @SideOnly(Side.CLIENT)
  private def isHittingPosition(pos: BlockPos): Boolean = {

    val itemstack: ItemStack = Minecraft.getMinecraft.thePlayer.getHeldItem
    var itemsAreEqual: Boolean = this.currentItemHittingBlock == null && itemstack == null

    if (this.currentItemHittingBlock != null && itemstack != null) {
      itemsAreEqual = (itemstack.getItem == this.currentItemHittingBlock.getItem) && ItemStack.areItemStackTagsEqual(itemstack, this.currentItemHittingBlock) && (itemstack.isItemStackDamageable || itemstack.getMetadata == this.currentItemHittingBlock.getMetadata)
    }

    pos == this.currentBlock && itemsAreEqual

  }

  @SideOnly(Side.CLIENT)
  private def clickBlockCreative(player: EntityPlayer, pos: BlockPos, side: EnumFacing) = {
    if (!shipWorld.extinguishFire(player, pos, side))
      onPlayerDestroyBlock(player, pos, side)
  }

  @SideOnly(Side.CLIENT)
  private def onPlayerDestroyBlock(player: EntityPlayer, pos: BlockPos, side: EnumFacing): Boolean = {
    if (currentGameType.isAdventure) {
      if (player.isSpectator) {
        return false
      }
      if (!player.isAllowEdit) {
        val block: Block = shipWorld.getBlockState(pos).getBlock
        val itemstack: ItemStack = player.getCurrentEquippedItem
        if (itemstack == null) {
          return false
        }
        if (!itemstack.canDestroy(block)) {
          return false
        }
      }
    }

    val stack: ItemStack = player.getCurrentEquippedItem
    if (stack != null && stack.getItem != null && stack.getItem.onBlockStartBreak(stack, pos, player)) {
      return false
    }

    if (player.capabilities.isCreativeMode && player.getHeldItem != null && player.getHeldItem.getItem.isInstanceOf[ItemSword]) {
      false
    }
    else {
      val iblockstate: IBlockState = shipWorld.getBlockState(pos)
      val block1: Block = iblockstate.getBlock
      if (block1.getMaterial == Material.air) {
        false
      }
      else {
        shipWorld.playAuxSFX(2001, pos, Block.getStateId(iblockstate))
        if (!player.capabilities.isCreativeMode) {
          val itemstack1: ItemStack = player.getCurrentEquippedItem
          if (itemstack1 != null) {
            itemstack1.onBlockDestroyed(shipWorld, block1, pos, player)
            if (itemstack1.stackSize == 0) {
              player.destroyCurrentEquippedItem()
            }
          }
        }
        val blockWasRemovedByPlayer = block1.removedByPlayer(shipWorld, pos, player, false)
        if (blockWasRemovedByPlayer) {
          block1.onBlockDestroyedByPlayer(shipWorld, pos, iblockstate)
        }
        blockWasRemovedByPlayer
      }
    }
  }

  @SideOnly(Side.CLIENT)
  private def resetBlockRemoving(player: EntityPlayer): Unit = {
    if (shipWorld == null) return
    shipWorld.Ship.InteractionHandler.sendBlockDiggingMessage(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, this.currentBlock, EnumFacing.DOWN)
    this.isHittingBlock = false
    this.curBlockDamageMP = 0.0F
    shipWorld.sendBlockBreakProgress(player.getEntityId, this.currentBlock, -1)
  }

  @SideOnly(Side.CLIENT)
  def sendClickBlockToController(player: EntityPlayer): Unit = {

    def leftClick = Minecraft.getMinecraft.currentScreen == null && Minecraft.getMinecraft.gameSettings.keyBindAttack.isKeyDown && Minecraft.getMinecraft.inGameHasFocus
    def shipMouseOver = shipWorld.Ship.InteractionHandler.getBlockPlayerIsLookingAt(1.0f)

    if (!leftClick)
      ClickSimulator.leftClickCounter = 0


    if (ClickSimulator.leftClickCounter <= 0 && !player.isUsingItem) {
      if (leftClick && shipMouseOver.isDefined) {
        val blockpos: BlockPos = shipMouseOver.get.getBlockPos
        if (shipWorld.getBlockState(blockpos).getBlock.getMaterial != Material.air && onPlayerDamageBlock(player, blockpos, shipMouseOver.get.sideHit)) {
          EffectRendererShip.addBlockHitEffects(shipWorld, blockpos, shipMouseOver.get)
          player.swingItem()
        }
      }
      else {
        resetBlockRemoving(player)
      }
    }
  }

  @SideOnly(Side.CLIENT)
  private def onPlayerDamageBlock(player: EntityPlayer, pos: BlockPos, side: EnumFacing): Boolean = {
    //Minecraft.getMinecraft.playerController.syncCurrentPlayItem
    if (this.blockHitDelay > 0) {
      this.blockHitDelay -= 1
      true
    }
    else if (this.currentGameType.isCreative && shipWorld.getWorldBorder.contains(pos)) {
      this.blockHitDelay = 5
      //this.netClientHandler.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, posBlock, directionFacing))
      shipWorld.Ship.InteractionHandler.sendBlockDiggingMessage(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, side)
      clickBlockCreative(player, pos, side)
      true
    }
    else if (this.isHittingPosition(pos)) {
      val block: Block = shipWorld.getBlockState(pos).getBlock
      if (block.getMaterial == Material.air) {
        this.isHittingBlock = false
        return false
      }

      this.curBlockDamageMP += block.getPlayerRelativeBlockHardness(player, shipWorld, pos)

      if (this.stepSoundTickCounter % 4.0F == 0.0F) {
        val worldPos = UnifiedPos.convertToWorld(pos, shipWorld.Ship.getPosition)
        Minecraft.getMinecraft.getSoundHandler.playSound(new PositionedSoundRecord(new ResourceLocation(block.stepSound.getStepSound), (block.stepSound.getVolume + 1.0F) / 8.0F, block.stepSound.getFrequency * 0.5F, worldPos.getX.toFloat + 0.5F, worldPos.getY.toFloat + 0.5F, worldPos.getZ.toFloat + 0.5F))
      }

      this.stepSoundTickCounter += 1
      if (this.curBlockDamageMP >= 1.0F) {
        this.isHittingBlock = false
        shipWorld.Ship.InteractionHandler.sendBlockDiggingMessage(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, side)
        //this.netClientHandler.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, posBlock, directionFacing))
        this.onPlayerDestroyBlock(player, pos, side)
        this.curBlockDamageMP = 0.0F
        this.stepSoundTickCounter = 0.0F
        this.blockHitDelay = 5
      }
      shipWorld.sendBlockBreakProgress(player.getEntityId, this.currentBlock, (this.curBlockDamageMP * 10.0F).toInt - 1)
      true

    }
    else
      clickBlock(player, pos, side)
  }

  @SideOnly(Side.CLIENT)
  def clickMouse(player: EntityPlayer): Unit = {
    if (ClickSimulator.leftClickCounter > 0) return

    player.swingItem()

    val mop = shipWorld.Ship.InteractionHandler.getBlockPlayerIsLookingAt(1.0f)


    if (mop.isEmpty) {
      if (!currentGameType.isCreative)
        ClickSimulator.leftClickCounter = 10
      return
    }

    val blockpos: BlockPos = mop.get.getBlockPos

    if (shipWorld.getBlockState(blockpos).getBlock.getMaterial != Material.air)
      clickBlock(player, blockpos, mop.get.sideHit)


  }

}
