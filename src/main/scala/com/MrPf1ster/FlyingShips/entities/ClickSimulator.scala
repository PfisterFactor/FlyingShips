package com.MrPf1ster.FlyingShips.entities

import com.MrPf1ster.FlyingShips.world.ShipWorld
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{ItemBlock, ItemStack, ItemSword}
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util._
import net.minecraft.world.WorldSettings
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by EJ on 7/3/2016.
  */

// This class is pretty much a carbon copy of PlayerControllerMP
@SideOnly(Side.CLIENT)
class ClickSimulator(ShipWorld:ShipWorld) {

  var leftClickCounter = 0


  private var currentBlock = new BlockPos(-1,-1,-1)
  private var currentItemHittingBlock:ItemStack = null
  private var curBlockDamageMP:Float = 0
  private var blockHitDelay = 5
  private var isHittingBlock:Boolean = false
  private def currentGameType:WorldSettings.GameType = Minecraft.getMinecraft.playerController.getCurrentGameType
  private var currentPlayerItem: Int = 0
  private var stepSoundTickCounter : Float = 0

  def simulateRightClick(player:EntityPlayer,pos:BlockPos,hitVec:Vec3,side:EnumFacing): Boolean = {

    def hitVecX = hitVec.xCoord.toFloat
    def hitVecY = hitVec.yCoord.toFloat
    def hitVecZ = hitVec.zCoord.toFloat

    def heldStack = player.getHeldItem

    val blockState = ShipWorld.getBlockState(pos)

    if (heldStack != null &&
      heldStack.getItem != null &&
      heldStack.getItem.onItemUseFirst(heldStack,player,ShipWorld,pos,side,hitVecX,hitVecY,hitVecZ)) {
      return true
    }

    var flag = false

    if (!player.isSneaking || player.getHeldItem == null || player.getHeldItem.getItem.doesSneakBypassUse(ShipWorld, pos, player)) {
      flag = blockState.getBlock.onBlockActivated(ShipWorld, pos, blockState, player, side, hitVec.xCoord.toFloat, hitVec.yCoord.toFloat, hitVec.zCoord.toFloat)
    }

    if (!flag && heldStack != null && heldStack.getItem.isInstanceOf[ItemBlock]) {

      val itemblock: ItemBlock = heldStack.getItem.asInstanceOf[ItemBlock]

      if (!itemblock.canPlaceBlockOnSide(ShipWorld, pos, side, player, heldStack))
        return false

    }

    ShipWorld.Ship.InteractionHandler.sendBlockPlacedMessage(pos,side,heldStack,hitVec)

    if (!flag && !player.isSpectator) {
      if (heldStack == null) {
        return false
      }
      else if (player.capabilities.isCreativeMode) {
        val meta: Int = heldStack.getMetadata
        val stackSize: Int = heldStack.stackSize
        val flag1: Boolean = heldStack.onItemUse(player, ShipWorld, pos, side, hitVecX, hitVecY, hitVecZ)
        heldStack.setItemDamage(meta)
        heldStack.stackSize = stackSize
        return flag1
      }
      else {
        if (!heldStack.onItemUse(player, ShipWorld, pos, side, hitVecX, hitVecY, hitVecZ))
          return false

        if (heldStack.stackSize <= 0) {
          net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, heldStack)
          player.destroyCurrentEquippedItem()
        }


        return true
      }
    }
    else {
      return true
    }
  }

  def simulateLeftClick(player:EntityPlayer,pos:BlockPos, side: EnumFacing): Boolean = {
    // Stops the player destroying the ship block
    if (pos == new BlockPos(0,0,0)) return false

    if (currentGameType.isAdventure) {
      if (currentGameType == WorldSettings.GameType.SPECTATOR) {
        return false
      }
      if (!player.isAllowEdit) {
        val block: Block = ShipWorld.getBlockState(pos).getBlock
        val itemstack: ItemStack = player.getCurrentEquippedItem
        if (itemstack == null) {
          return false
        }
        if (!itemstack.canDestroy(block)) {
          return false
        }
      }
    }

    if (!ShipWorld.getWorldBorder.contains(pos)) {
      return false
    }
    else {
      if (currentGameType.isCreative) {
        ShipWorld.Ship.InteractionHandler.sendBlockDiggingMessage(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,pos,side)
        clickBlockCreative(player,pos,side)
        this.blockHitDelay = 5
      }
      else if (!this.isHittingBlock /* isHitting Block */ || !isHittingPosition(pos)) {
        if (this.isHittingBlock) {
          ShipWorld.Ship.InteractionHandler.sendBlockDiggingMessage(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK,pos,side)
        }
        ShipWorld.Ship.InteractionHandler.sendBlockDiggingMessage(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,pos,side)
        val block1: Block = ShipWorld.getBlockState(pos).getBlock
        val flag: Boolean = block1.getMaterial != Material.air
        if (flag && this.curBlockDamageMP == 0.0F) {
          block1.onBlockClicked(ShipWorld, pos, Minecraft.getMinecraft.thePlayer)
        }
        if (flag && block1.getPlayerRelativeBlockHardness( Minecraft.getMinecraft.thePlayer, ShipWorld, pos) >= 1.0F) {
          this.onPlayerDestroyBlock(player,pos, side)
        }
        else {
          this.isHittingBlock = true
          this.currentBlock = pos
          this.currentItemHittingBlock =  Minecraft.getMinecraft.thePlayer.getHeldItem
          this.curBlockDamageMP = 0.0F
          this.stepSoundTickCounter = 0.0F
          ShipWorld.sendBlockBreakProgress(Minecraft.getMinecraft.thePlayer.getEntityId, this.currentBlock, (this.curBlockDamageMP * 10.0F).toInt - 1)
        }
      }
      return true
    }

  }

  private def isHittingPosition(pos:BlockPos): Boolean = {

    val itemstack: ItemStack = Minecraft.getMinecraft.thePlayer.getHeldItem
    var flag: Boolean = this.currentItemHittingBlock == null && itemstack == null

    if (this.currentItemHittingBlock != null && itemstack != null) {
      flag = (itemstack.getItem == this.currentItemHittingBlock.getItem) && ItemStack.areItemStackTagsEqual(itemstack, this.currentItemHittingBlock) && (itemstack.isItemStackDamageable || itemstack.getMetadata == this.currentItemHittingBlock.getMetadata)
    }

    return pos == this.currentBlock && flag

  }

  private def clickBlockCreative(player:EntityPlayer, pos:BlockPos, side:EnumFacing) = {
    if (!ShipWorld.extinguishFire(player, pos, side))
      onPlayerDestroyBlock(player,pos,side)
  }

  private def onPlayerDestroyBlock(player:EntityPlayer,pos: BlockPos, side: EnumFacing): Boolean = {
    if (false /* player.isInAdventureMode */) {
      if (player.isSpectator) {
        return false
      }
      if (!player.isAllowEdit) {
        val block: Block = ShipWorld.getBlockState(pos).getBlock
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
      return false
    }
    else {
      val iblockstate: IBlockState = ShipWorld.getBlockState(pos)
      val block1: Block = iblockstate.getBlock
      if (block1.getMaterial == Material.air) {
        return false
      }
      else {
        ShipWorld.playAuxSFX(2001, pos, Block.getStateId(iblockstate))
        if (!player.capabilities.isCreativeMode) {
          val itemstack1: ItemStack = player.getCurrentEquippedItem
          if (itemstack1 != null) {
            itemstack1.onBlockDestroyed(ShipWorld, block1, pos, player)
            if (itemstack1.stackSize == 0) {
              player.destroyCurrentEquippedItem
            }
          }
        }
        val flag: Boolean = block1.removedByPlayer(ShipWorld, pos, player, false)
        if (flag) {
          block1.onBlockDestroyedByPlayer(ShipWorld, pos, iblockstate)
        }
        return flag
      }
    }
  }

  private def resetBlockRemoving(player:EntityPlayer) = {
    if (true) {
      ShipWorld.Ship.InteractionHandler.sendBlockDiggingMessage(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK,this.currentBlock,EnumFacing.DOWN)
      // this.netClientHandler.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, this.currentBlock, EnumFacing.DOWN))
      this.isHittingBlock = false
      this.curBlockDamageMP = 0.0F
      ShipWorld.sendBlockBreakProgress(player.getEntityId, this.currentBlock, -1)
    }
  }

   def sendClickBlockToController(player:EntityPlayer):Unit = {

    def leftClick = Minecraft.getMinecraft.currentScreen == null && Minecraft.getMinecraft.gameSettings.keyBindAttack.isKeyDown && Minecraft.getMinecraft.inGameHasFocus
    def objectMouseOver = Minecraft.getMinecraft.objectMouseOver

      if (!leftClick) {
        this.leftClickCounter = 0
        resetBlockRemoving(player)
      }

    if (this.leftClickCounter <= 0 && !player.isUsingItem) {
      if (leftClick && objectMouseOver != null && objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && objectMouseOver.entityHit.isInstanceOf[EntityShip]) {
        val hitInfo = ShipWorld.Ship.InteractionHandler.getBlockPlayerIsLookingAt(1.0f)
        if (hitInfo.isEmpty){
          resetBlockRemoving(player)
          return
        }

        val blockpos: BlockPos = hitInfo.get.getBlockPos
        if (ShipWorld.getBlockState(blockpos).getBlock.getMaterial != Material.air && onPlayerDamageBlock(player,blockpos,hitInfo.get.sideHit)) {
          Minecraft.getMinecraft.effectRenderer.addBlockHitEffects(blockpos, hitInfo.get)
          player.swingItem
        }
      }
      else {
        resetBlockRemoving(player)
      }
    }
  }

  private def onPlayerDamageBlock(player:EntityPlayer,pos:BlockPos, side:EnumFacing): Boolean = {
    //Minecraft.getMinecraft.playerController.syncCurrentPlayItem
    if (this.blockHitDelay > 0) {
      this.blockHitDelay -= 1
      return true
    }
    else if (this.currentGameType.isCreative && ShipWorld.getWorldBorder.contains(pos)) {
      this.blockHitDelay = 5
      //this.netClientHandler.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, posBlock, directionFacing))
      ShipWorld.Ship.InteractionHandler.sendBlockDiggingMessage(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,pos,side)
      clickBlockCreative(player,pos,side)
      return true
    }
    else if (this.isHittingPosition(pos)) {
      val block: Block = ShipWorld.getBlockState(pos).getBlock
      if (block.getMaterial == Material.air) {
        this.isHittingBlock = false
        return false
      }
      else {
        this.curBlockDamageMP += block.getPlayerRelativeBlockHardness(player, ShipWorld, pos)
        if (this.stepSoundTickCounter % 4.0F == 0.0F) {
          // Fix for world position potentially
          Minecraft.getMinecraft.getSoundHandler.playSound(new PositionedSoundRecord(new ResourceLocation(block.stepSound.getStepSound), (block.stepSound.getVolume + 1.0F) / 8.0F, block.stepSound.getFrequency * 0.5F, pos.getX.toFloat + 0.5F, pos.getY.toFloat + 0.5F, pos.getZ.toFloat + 0.5F))
        }
        this.stepSoundTickCounter += 1
        if (this.curBlockDamageMP >= 1.0F) {
          this.isHittingBlock = false
          ShipWorld.Ship.InteractionHandler.sendBlockDiggingMessage(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, side)
          //this.netClientHandler.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, posBlock, directionFacing))
          this.onPlayerDestroyBlock(player,pos,side)
          this.curBlockDamageMP = 0.0F
          this.stepSoundTickCounter = 0.0F
          this.blockHitDelay = 5
        }
        ShipWorld.sendBlockBreakProgress(player.getEntityId, this.currentBlock, (this.curBlockDamageMP * 10.0F).toInt - 1)
        return true
      }
    }
    else {
      return simulateLeftClick(player,pos,side)
    }
  }


}
