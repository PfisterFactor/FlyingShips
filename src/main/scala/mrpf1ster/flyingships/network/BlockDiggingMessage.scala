package mrpf1ster.flyingships.network

import io.netty.buffer.ByteBuf
import mrpf1ster.flyingships.util.ShipLocator
import mrpf1ster.flyingships.world.ShipWorld
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.item.ItemStack
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{BlockPos, EnumFacing}
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by EJ on 7/3/2016.
  */
class BlockDiggingMessage(status: C07PacketPlayerDigging.Action, pos: BlockPos, facing: EnumFacing, shipID: Int) extends IMessage {
  def this() = this(null, null, null, -1)

  var Status = if (status != null) status else C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK
  var BlockPosition: BlockPos = if (pos != null) pos else new BlockPos(-1, -1, -1)
  var Side: EnumFacing = if (facing != null) facing else EnumFacing.values()(0)
  var ShipID = shipID

  override def toBytes(buf: ByteBuf): Unit = {
    // Status
    buf.writeInt(Status.ordinal())

    // BlockPos
    buf.writeLong(BlockPosition.toLong)

    // Side
    buf.writeInt(Side.getIndex)

    // ShipID
    buf.writeInt(shipID)
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    // Status
    Status = C07PacketPlayerDigging.Action.values()(buf.readInt())

    // BlockPos
    BlockPosition = BlockPos.fromLong(buf.readLong())

    // Side
    Side = EnumFacing.values()(buf.readInt())

    // ShipID
    ShipID = buf.readInt()
  }

}

class ServerBlockDiggingMessageHandler extends IMessageHandler[BlockDiggingMessage, IMessage] {

  @Override
  override def onMessage(message: BlockDiggingMessage, ctx: MessageContext): IMessage = {
    if (message.ShipID == -1) return null
    if (message.BlockPosition == new BlockPos(0, 0, 0)) return null

    Minecraft.getMinecraft.addScheduledTask(new BlockDiggingMessageTask(message, ctx))
    null
  }

  class BlockDiggingMessageTask(message: BlockDiggingMessage, ctx: MessageContext) extends Runnable {
    val Message = message
    val Context = ctx

    // On Server
    override def run(): Unit = {


      def player = ctx.getServerHandler.playerEntity

      val Ship = ShipLocator.getShip(player.worldObj, message.ShipID)

      if (Ship.isEmpty)
        return

      processPacket(ctx.getServerHandler, Ship.get.ShipWorld)
    }

    def processPacket(netHandlerPlayServer: NetHandlerPlayServer, shipWorld: ShipWorld): Unit = {
      def player = netHandlerPlayServer.playerEntity

      val blockpos: BlockPos = message.BlockPosition
      player.markPlayerActive()


      message.Status match {
        case Action.DROP_ITEM => if (!player.isSpectator) player.dropOneItem(false)

        case Action.DROP_ALL_ITEMS => if (!player.isSpectator) player.dropOneItem(true)

        case Action.RELEASE_USE_ITEM => player.stopUsingItem()

        case Action.START_DESTROY_BLOCK => oddSwitchSyntax()
        case Action.ABORT_DESTROY_BLOCK => oddSwitchSyntax()
        case Action.STOP_DESTROY_BLOCK => oddSwitchSyntax()
        case _ => throw new IllegalArgumentException("Invalid player action")
      }

      def oddSwitchSyntax(): Unit = {
        if (message.Status == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
          if (shipWorld.getWorldBorder.contains(blockpos)) {
            ItemInWorldManagerFaker.onBlockClicked(player, blockpos, message.Side, shipWorld)
          }
          else {
            // Handled by Shipworld
            //player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(worldserver, blockpos))
          }
        }
        else {
          if (message.Status == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
            ItemInWorldManagerFaker.blockRemoving(player,blockpos,shipWorld)
          }
          else if (message.Status == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) {
            shipWorld.sendBlockBreakProgress(player.getEntityId,blockpos,-1)
          }
          if (shipWorld.getBlockState(blockpos).getBlock.getMaterial != Material.air) {
            // Handled by Shipworld
            //player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(worldserver, blockpos))
          }
        }
      }
    }


  }

}

private object ItemInWorldManagerFaker {
  def removeBlock(player:EntityPlayerMP, pos:BlockPos, canHarvest:Boolean, shipWorld: ShipWorld): Boolean = {
    val iblockstate: IBlockState = shipWorld.getBlockState(pos)

    iblockstate.getBlock.onBlockHarvested(shipWorld, pos, iblockstate, player)

    val blockWasRemoved: Boolean = iblockstate.getBlock.removedByPlayer(shipWorld, pos, player, canHarvest)

    if (blockWasRemoved)
      iblockstate.getBlock.onBlockDestroyedByPlayer(shipWorld, pos, iblockstate)

    blockWasRemoved
  }

  def tryHarvestBlock(player:EntityPlayerMP, pos: BlockPos, shipWorld: ShipWorld): Boolean = {
    val exp: Int = net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(shipWorld, player.theItemInWorldManager.getGameType, player, pos)

    if (exp == -1) return false

    val iblockstate: IBlockState = shipWorld.getBlockState(pos)
    val tileentity: TileEntity = shipWorld.getTileEntity(pos)

    val stack: ItemStack = player.getCurrentEquippedItem
    if (stack != null && stack.getItem.onBlockStartBreak(stack, pos, player)) return false
    shipWorld.playAuxSFXAtEntity(player, 2001, pos, Block.getStateId(iblockstate))
    var blockWasRemoved: Boolean = false
    if (player.capabilities.isCreativeMode) {
      blockWasRemoved = removeBlock(player,pos,canHarvest = false,shipWorld)
    }
    else {
      val itemstack1: ItemStack = player.getCurrentEquippedItem
      val blockCanBeHarvested: Boolean = iblockstate.getBlock.canHarvestBlock(shipWorld, pos, player)
      if (itemstack1 != null) {
        itemstack1.onBlockDestroyed(shipWorld, iblockstate.getBlock, pos, player)
        if (itemstack1.stackSize == 0) {
          player.destroyCurrentEquippedItem()
        }
      }
      blockWasRemoved = this.removeBlock(player,pos, blockCanBeHarvested, shipWorld)
      if (blockWasRemoved && blockCanBeHarvested) {
        iblockstate.getBlock.harvestBlock(shipWorld, player, pos, iblockstate, tileentity)
      }
    }
    if (!player.capabilities.isCreativeMode && blockWasRemoved && exp > 0) {
      iblockstate.getBlock.dropXpOnBlockBreak(shipWorld, pos, exp)
    }
    blockWasRemoved
  }

  def onBlockClicked(player: EntityPlayerMP, pos: BlockPos, side: EnumFacing, shipWorld: ShipWorld): Unit = {
    val event: PlayerInteractEvent = net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(player, net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.LEFT_CLICK_BLOCK, shipWorld, pos, side)
    if (player.capabilities.isCreativeMode) {
      if (!shipWorld.extinguishFire(null.asInstanceOf[EntityPlayer], pos, side)) {
        this.tryHarvestBlock(player,pos,shipWorld)
      }
    }
    else {
      val block: Block = shipWorld.getBlockState(pos).getBlock
      if (player.theItemInWorldManager.getGameType.isAdventure) {
        if (player.isSpectator) {
          return
        }
        if (!player.isAllowEdit) {
          val itemstack: ItemStack = player.getCurrentEquippedItem
          if (itemstack == null) {
            return
          }
          if (!itemstack.canDestroy(block)) {
            return
          }
        }
      }

      //this.initialDamage = this.curblockDamage
      var f: Float = 1.0F
      if (!block.isAir(shipWorld, pos)) {
        if (event.useBlock != net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) {
          block.onBlockClicked(shipWorld, pos, player)
          shipWorld.extinguishFire(null.asInstanceOf[EntityPlayer], pos, side)
        }
        else {
          //player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(theWorld, pos))
        }
        f = block.getPlayerRelativeBlockHardness(player, shipWorld, pos)
      }
      if (event.useItem == net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) {
        if (f >= 1.0F) {
          //player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(shipWorld, pos))
        }
        return
      }
      if (!block.isAir(shipWorld, pos) && f >= 1.0F) {
        tryHarvestBlock(player,pos,shipWorld)
      }
      else {
        //this.isDestroyingBlock = true
        //this.field_180240_f = pos
        val i: Int = (f * 10.0F).toInt
        shipWorld.sendBlockBreakProgress(player.getEntityId, pos, i)
        //this.durabilityRemainingOnBlock = i
      }
    }
  }

  def blockRemoving(player: EntityPlayerMP, pos: BlockPos, shipWorld: ShipWorld) = {
    if (true /* pos == this.field_180240_f */) {
     // val i: Int = this.curblockDamage - this.initialDamage
      val block: Block = shipWorld.getBlockState(pos).getBlock
      if (!block.isAir(shipWorld, pos)) {
        //val f: Float = block.getPlayerRelativeBlockHardness(this.thisPlayerMP, this.thisPlayerMP.worldObj, pos) * (i + 1).toFloat
        shipWorld.sendBlockBreakProgress(player.getEntityId, pos, -1)
        tryHarvestBlock(player,pos,shipWorld)
      }
    }
  }
}
