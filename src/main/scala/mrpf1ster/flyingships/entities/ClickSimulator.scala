package mrpf1ster.flyingships.entities

import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.network.PacketSender
import mrpf1ster.flyingships.util.{ShipLocator, UnifiedPos}
import mrpf1ster.flyingships.world.{ShipWorld, ShipWorldClient}
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.item.{ItemBlock, ItemStack, ItemSword}
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util._
import net.minecraft.world.WorldSettings
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by EJ on 7/3/2016.
  */

// This class handles all the mouse input
// It spoofs it on the shipworld
// Lot of the code is just taken straight from PlayerControllerMP
object ClickSimulator {

  var leftClickCounter = 0
  var rightClickDelayTimer = 0

  private var currentBlock = new BlockPos(-1, -1, -1)
  private var currentItemHittingBlock: ItemStack = null
  private var curBlockDamageMP: Float = 0
  private var blockHitDelay = 5
  private var isHittingBlock: Boolean = false

  private def currentGameType: WorldSettings.GameType = Minecraft.getMinecraft.playerController.getCurrentGameType

  private var stepSoundTickCounter: Float = 0

  private var doLeftClick = false
  private var doRightClick = false

  private def thePlayer = Minecraft.getMinecraft.thePlayer

  def handleMousePress(): Unit = {
    val leftMouseButtonDown = Minecraft.getMinecraft.gameSettings.keyBindAttack.isKeyDown
    val rightMouseButtonDown = Minecraft.getMinecraft.gameSettings.keyBindUseItem.isKeyDown

    if (!leftMouseButtonDown)
      doLeftClick = false
    if (!rightMouseButtonDown)
      doRightClick = false

    val shipInteractedWith = ShipLocator.getClientShip(ShipWorld.ShipMouseOverID)

    if (Minecraft.getMinecraft.theWorld != null && shipInteractedWith.isDefined) {
      val shipWorld = shipInteractedWith.get.Shipworld.asInstanceOf[ShipWorldClient]

      if (leftMouseButtonDown && !doLeftClick)
        ClickSimulator.clickMouse(shipWorld)

      if (rightMouseButtonDown && !doRightClick)
        ClickSimulator.rightClickMouse(shipWorld)

    }

    if (leftMouseButtonDown)
      doLeftClick = true
    if (rightMouseButtonDown)
      doRightClick = true
  }

  def onClientTick() = {
    if (Minecraft.getMinecraft.currentScreen != null)
      ClickSimulator.leftClickCounter = 10000

    if (ClickSimulator.leftClickCounter > 0)
      ClickSimulator.leftClickCounter -= 1

    if (ClickSimulator.rightClickDelayTimer > 0)
      ClickSimulator.rightClickDelayTimer -= 1
  }

  @SideOnly(Side.CLIENT)
  def rightClickMouse(shipWorld: ShipWorldClient): Unit = {
    if (!isHittingBlock) {
      var flag: Boolean = true
      val itemstack: ItemStack = Minecraft.getMinecraft.thePlayer.inventory.getCurrentItem

      val mop = ShipInteractionHelper.getBlockPlayerIsLookingAt(shipWorld.Ship.ShipID)

      if (mop.isEmpty) return
      ClickSimulator.rightClickDelayTimer = 4
      val blockpos: BlockPos = mop.get.getBlockPos

      if (!shipWorld.isAirBlock(blockpos)) {
        val i: Int = if (itemstack != null) itemstack.stackSize else 0

        val result: Boolean = !net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(thePlayer, net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, shipWorld, blockpos, mop.get.sideHit).isCanceled
        if (result) {
          if (onPlayerRightClick(shipWorld, blockpos, mop.get.hitVec, mop.get.sideHit)) {
            flag = false
            thePlayer.swingItem()
          }
        }
        if (itemstack == null) {
          return
        }
        if (itemstack.stackSize == 0) {
          thePlayer.inventory.mainInventory(thePlayer.inventory.currentItem) = null
        }
        else if (itemstack.stackSize != i || thePlayer.capabilities.isCreativeMode) {
          Minecraft.getMinecraft.entityRenderer.itemRenderer.resetEquippedProgress()
        }
      }

      if (flag) {
        val itemstack1: ItemStack = thePlayer.inventory.getCurrentItem
        val result: Boolean = !net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(thePlayer, net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_AIR, Minecraft.getMinecraft.theWorld, null, null).isCanceled
        if (result && itemstack1 != null && sendUseItem(shipWorld, itemstack1)) {
          Minecraft.getMinecraft.entityRenderer.itemRenderer.resetEquippedProgress2()
        }
      }
    }
  }

  @SideOnly(Side.CLIENT)
  def onPlayerRightClick(shipWorld: ShipWorldClient, pos: BlockPos, hitVec: Vec3, side: EnumFacing): Boolean = {

    def hitVecX = hitVec.xCoord.toFloat
    def hitVecY = hitVec.yCoord.toFloat
    def hitVecZ = hitVec.zCoord.toFloat

    def heldStack = thePlayer.inventory.getCurrentItem

    val blockState = shipWorld.getBlockState(pos)

    if (heldStack != null &&
      heldStack.getItem != null &&
      heldStack.getItem.onItemUseFirst(heldStack, thePlayer, shipWorld, pos, side, hitVecX, hitVecY, hitVecZ)) {
      return true
    }

    var blockWasActivated = false

    if (!thePlayer.isSneaking || thePlayer.getHeldItem == null || thePlayer.getHeldItem.getItem.doesSneakBypassUse(shipWorld, pos, thePlayer))
      blockWasActivated = blockState.getBlock.onBlockActivated(shipWorld, pos, blockState, thePlayer, side, hitVec.xCoord.toFloat, hitVec.yCoord.toFloat, hitVec.zCoord.toFloat)

    if (!blockWasActivated && heldStack != null && heldStack.getItem.isInstanceOf[ItemBlock]) {

      val itemblock: ItemBlock = heldStack.getItem.asInstanceOf[ItemBlock]

      if (!itemblock.canPlaceBlockOnSide(shipWorld, pos, side, thePlayer, heldStack))
        return false
    }

    ShipInteractionHelper.sendBlockPlacedMessage(shipWorld.Ship.ShipID, pos, side, heldStack, hitVec)

    if (blockWasActivated && !thePlayer.isSpectator) return true

    if (heldStack == null) return false

    // If the player is in creative, use the item without damaging it or lowering its stacksize
    if (thePlayer.capabilities.isCreativeMode) {
      val meta: Int = heldStack.getMetadata
      val stackSize: Int = heldStack.stackSize
      val itemWasUsed: Boolean = heldStack.onItemUse(thePlayer, shipWorld, pos, side, hitVecX, hitVecY, hitVecZ)
      heldStack.setItemDamage(meta)
      heldStack.stackSize = stackSize
      return itemWasUsed
    }

    val itemWasUsed = heldStack.onItemUse(thePlayer, shipWorld, pos, side, hitVecX, hitVecY, hitVecZ)

    if (!itemWasUsed)
      return false

    if (heldStack.stackSize <= 0) {
      net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(thePlayer, heldStack)
      thePlayer.destroyCurrentEquippedItem()
    }

    true

  }

  def sendUseItem(shipWorld: ShipWorldClient, itemStackIn: ItemStack): Boolean = {
    if (this.currentGameType == WorldSettings.GameType.SPECTATOR) {
      false
    }
    else {
      ShipInteractionHelper.sendBlockPlacedMessage(shipWorld.Ship.ShipID, thePlayer.inventory.getCurrentItem)
      val i: Int = itemStackIn.stackSize
      val itemstack: ItemStack = itemStackIn.useItemRightClick(shipWorld, thePlayer)
      if (itemstack != itemStackIn || itemstack != null && itemstack.stackSize != i) {
        thePlayer.inventory.mainInventory(thePlayer.inventory.currentItem) = itemstack
        if (itemstack.stackSize <= 0) {
          thePlayer.inventory.mainInventory(thePlayer.inventory.currentItem) = null
          net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(thePlayer, itemstack)
        }
        true
      }
      else {
        false
      }
    }
  }


  @SideOnly(Side.CLIENT)
  def clickBlock(shipWorld: ShipWorldClient, pos: BlockPos, side: EnumFacing): Boolean = {
    // No Left clicking for spectators
    if (currentGameType == WorldSettings.GameType.SPECTATOR) return false

    if (currentGameType.isAdventure) {

      if (!thePlayer.isAllowEdit) {
        val block: Block = shipWorld.getBlockState(pos).getBlock
        val itemstack: ItemStack = thePlayer.getCurrentEquippedItem

        if (itemstack == null) return false
        if (!itemstack.canDestroy(block)) return false
      }
    }

    if (!shipWorld.getWorldBorder.contains(pos)) return false

    if (currentGameType.isCreative) {
      ShipInteractionHelper.sendBlockDiggingMessage(shipWorld.Ship.ShipID, C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, side)
      clickBlockCreative(shipWorld, pos, side)
      this.blockHitDelay = 5
      return true
    }

    if (!this.isHittingBlock || !isHittingPosition(pos)) {
      if (this.isHittingBlock)
        ShipInteractionHelper.sendBlockDiggingMessage(shipWorld.Ship.ShipID, C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, pos, side)

      ShipInteractionHelper.sendBlockDiggingMessage(shipWorld.Ship.ShipID, C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, side)
      val block1: Block = shipWorld.getBlockState(pos).getBlock
      val blockIsNotAir: Boolean = block1.getMaterial != Material.air

      if (blockIsNotAir && this.curBlockDamageMP == 0.0F)
        block1.onBlockClicked(shipWorld, pos, Minecraft.getMinecraft.thePlayer)

      if (blockIsNotAir && block1.getPlayerRelativeBlockHardness(Minecraft.getMinecraft.thePlayer, shipWorld, pos) >= 1.0F) {
        this.onPlayerDestroyBlock(shipWorld, pos, side)
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
  private def clickBlockCreative(shipWorld: ShipWorldClient, pos: BlockPos, side: EnumFacing) = {
    if (!shipWorld.extinguishFire(thePlayer, pos, side))
      onPlayerDestroyBlock(shipWorld, pos, side)
  }

  @SideOnly(Side.CLIENT)
  private def onPlayerDestroyBlock(shipWorld: ShipWorldClient, pos: BlockPos, side: EnumFacing): Boolean = {
    if (currentGameType.isAdventure) {
      if (thePlayer.isSpectator) {
        return false
      }
      if (!thePlayer.isAllowEdit) {
        val block: Block = shipWorld.getBlockState(pos).getBlock
        val itemstack: ItemStack = thePlayer.getCurrentEquippedItem
        if (itemstack == null) {
          return false
        }
        if (!itemstack.canDestroy(block)) {
          return false
        }
      }
    }

    val stack: ItemStack = thePlayer.getCurrentEquippedItem
    if (stack != null && stack.getItem != null && stack.getItem.onBlockStartBreak(stack, pos, thePlayer)) {
      return false
    }

    if (thePlayer.capabilities.isCreativeMode && thePlayer.getHeldItem != null && thePlayer.getHeldItem.getItem.isInstanceOf[ItemSword]) {
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
        if (!thePlayer.capabilities.isCreativeMode) {
          val itemstack1: ItemStack = thePlayer.getCurrentEquippedItem
          if (itemstack1 != null) {
            itemstack1.onBlockDestroyed(shipWorld, block1, pos, thePlayer)
            if (itemstack1.stackSize == 0) {
              thePlayer.destroyCurrentEquippedItem()
            }
          }
        }
        val blockWasRemovedByPlayer = block1.removedByPlayer(shipWorld, pos, thePlayer, false)
        if (blockWasRemovedByPlayer) {
          block1.onBlockDestroyedByPlayer(shipWorld, pos, iblockstate)
        }
        blockWasRemovedByPlayer
      }
    }
  }

  @SideOnly(Side.CLIENT)
  private def resetBlockRemoving(shipWorld: ShipWorldClient): Unit = {
    if (shipWorld == null) return
    if (!isHittingBlock) return
    ShipInteractionHelper.sendBlockDiggingMessage(shipWorld.Ship.ShipID, C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, this.currentBlock, EnumFacing.DOWN)
    this.isHittingBlock = false
    this.curBlockDamageMP = 0.0F
    shipWorld.sendBlockBreakProgress(thePlayer.getEntityId, this.currentBlock, -1)
  }

  @SideOnly(Side.CLIENT)
  def sendClickBlockToController(shipWorld: ShipWorldClient): Unit = {

    def leftClick = Minecraft.getMinecraft.currentScreen == null && Minecraft.getMinecraft.gameSettings.keyBindAttack.isKeyDown && Minecraft.getMinecraft.inGameHasFocus
    def shipMouseOver = ShipInteractionHelper.getBlockPlayerIsLookingAt(shipWorld.Ship.ShipID)

    if (!leftClick) {
      ClickSimulator.leftClickCounter = 0
    }



    if (ClickSimulator.leftClickCounter <= 0 && !thePlayer.isUsingItem) {
      if (leftClick && shipMouseOver.isDefined) {
        val blockpos: BlockPos = shipMouseOver.get.getBlockPos
        if (shipWorld.getBlockState(blockpos).getBlock.getMaterial != Material.air && onPlayerDamageBlock(shipWorld, blockpos, shipMouseOver.get.sideHit)) {
          EffectRendererShip.addBlockHitEffects(shipWorld, blockpos, shipMouseOver.get)
          thePlayer.swingItem()
        }
      }
      else
        resetBlockRemoving(shipWorld)
    }

  }

  @SideOnly(Side.CLIENT)
  private def onPlayerDamageBlock(shipWorld: ShipWorldClient, pos: BlockPos, side: EnumFacing): Boolean = {
    //Minecraft.getMinecraft.playerController.syncCurrentPlayItem
    if (this.blockHitDelay > 0) {
      this.blockHitDelay -= 1
      true
    }
    else if (this.currentGameType.isCreative && shipWorld.getWorldBorder.contains(pos)) {
      this.blockHitDelay = 5
      //this.netClientHandler.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, posBlock, directionFacing))
      ShipInteractionHelper.sendBlockDiggingMessage(shipWorld.Ship.ShipID, C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, side)
      clickBlockCreative(shipWorld, pos, side)
      true
    }
    else if (this.isHittingPosition(pos)) {
      val block: Block = shipWorld.getBlockState(pos).getBlock
      if (block.getMaterial == Material.air) {
        this.isHittingBlock = false
        return false
      }

      this.curBlockDamageMP += block.getPlayerRelativeBlockHardness(thePlayer, shipWorld, pos)

      if (this.stepSoundTickCounter % 4.0F == 0.0F) {
        val worldPos = UnifiedPos.convertToWorld(pos, shipWorld.Ship.getPosition)
        Minecraft.getMinecraft.getSoundHandler.playSound(new PositionedSoundRecord(new ResourceLocation(block.stepSound.getStepSound), (block.stepSound.getVolume + 1.0F) / 8.0F, block.stepSound.getFrequency * 0.5F, worldPos.getX.toFloat + 0.5F, worldPos.getY.toFloat + 0.5F, worldPos.getZ.toFloat + 0.5F))
      }

      this.stepSoundTickCounter += 1
      if (this.curBlockDamageMP >= 1.0F) {
        this.isHittingBlock = false
        ShipInteractionHelper.sendBlockDiggingMessage(shipWorld.Ship.ShipID, C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, side)
        //this.netClientHandler.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, posBlock, directionFacing))
        this.onPlayerDestroyBlock(shipWorld, pos, side)
        this.curBlockDamageMP = 0.0F
        this.stepSoundTickCounter = 0.0F
        this.blockHitDelay = 5
      }
      shipWorld.sendBlockBreakProgress(thePlayer.getEntityId, this.currentBlock, (this.curBlockDamageMP * 10.0F).toInt - 1)
      true

    }
    else
      clickBlock(shipWorld, pos, side)
  }

  @SideOnly(Side.CLIENT)
  def clickMouse(shipWorld: ShipWorldClient): Unit = {
    if (ClickSimulator.leftClickCounter > 0) return

    thePlayer.swingItem()

    val mop = ShipInteractionHelper.getBlockPlayerIsLookingAt(shipWorld.Ship.ShipID)


    if (mop.isEmpty) {
      if (!currentGameType.isCreative)
        ClickSimulator.leftClickCounter = 10
      return
    }

    val blockpos: BlockPos = mop.get.getBlockPos

    if (shipWorld.getBlockState(blockpos).getBlock.getMaterial != Material.air)
      clickBlock(shipWorld, blockpos, mop.get.sideHit)


  }

  object ShipInteractionHelper {

    // Gets the block the mouse is over on the passed ship entity
    @SideOnly(Side.CLIENT)
    def getBlockPlayerIsLookingAt(shipID: Int): Option[MovingObjectPosition] = {

      // Gets the ship our mouse is over
      val shipMouseOver = ShipWorld.ShipMouseOver
      val shipMouseOverID = ShipWorld.ShipMouseOverID

      if (shipMouseOverID != shipID || shipMouseOver == null || shipMouseOver.typeOfHit != MovingObjectType.BLOCK)
        None
      else
        Some(shipMouseOver)
    }

    @SideOnly(Side.CLIENT)
    def sendBlockDiggingMessage(shipID: Int, status: C07PacketPlayerDigging.Action, pos: BlockPos, side: EnumFacing) = {
      PacketSender.sendBlockDiggingPacket(shipID,status,pos,side).toServer()
    }

    @SideOnly(Side.CLIENT)
    def sendBlockPlacedMessage(shipID: Int, pos: BlockPos, side: EnumFacing, heldItem: ItemStack, hitVec: Vec3) = {
      PacketSender.sendBlockPlacedPacket(shipID,pos,side,heldItem,hitVec).toServer()
    }

    @SideOnly(Side.CLIENT)
    def sendBlockPlacedMessage(shipID: Int, itemStack: ItemStack) = {
      PacketSender.sendBlockPlacedPacket(shipID,new BlockPos(-1,-1,-1),null,itemStack,new Vec3(-1,-1,-1)).toServer()
    }

    // Code adapted from https://bitbucket.org/cuchaz/mod-shared/
    // Gets the reach distance of the player
    def getPlayerReachDistance(player: EntityPlayer): Double = player match {
      case playerMP: EntityPlayerMP => playerMP.theItemInWorldManager.getBlockReachDistance
      case _: AbstractClientPlayer => Minecraft.getMinecraft.playerController.getBlockReachDistance()
      case _ => 0

    }

  }

}
