package mrpf1ster.flyingships.network

import javax.vecmath.Quat4f

import com.unascribed.lambdanetwork.{BiConsumer, Token}
import io.netty.buffer.{ByteBuf, Unpooled}
import mrpf1ster.flyingships.FlyingShips
import mrpf1ster.flyingships.entities.{EntityShip, EntityShipTracker}
import mrpf1ster.flyingships.util.ShipLocator
import mrpf1ster.flyingships.world.ShipWorld
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action
import net.minecraft.network.play.server.{S2FPacketSetSlot, S35PacketUpdateTileEntity}
import net.minecraft.tileentity._
import net.minecraft.util.{BlockPos, EnumFacing, Vec3, Vec3i}
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.network.ByteBufUtils

import scala.util.Try

/**
  * Created by EJPfi on 1/13/2017.
  */
// Contains all the code for handling our packets
object PacketHandlers {

  // Called by all our messages to check the ship entity for validity
  // Logs a message to console if something went wrong
  // Returns true on null
  def isNullOrEmpty(ship: Option[EntityShip], caller: String, shipID: Int): Boolean = {
    if (ship.isEmpty) {
      FlyingShips.logger.warn(s"$caller: Ship ID $shipID was not located! Aborting!")
      return true
    }
    if (ship.get.Shipworld == null) {
      FlyingShips.logger.warn(s"$caller: Ship ID $shipID's Shipworld was null! Aborting!")
      return true
    }
    false
  }

  // Handlers
  object BlockActionHandler extends BiConsumer[EntityPlayer,Token] {
    override def accept(player: EntityPlayer, token: Token): Unit = {
      val shipID = token.getInt("ShipID")
      val blockPos = BlockPos.fromLong(token.getLong("BlockPosition"))
      val data1 = token.getInt("Data1")
      val data2 = token.getInt("Data2")
      val block = Block.getBlockById(token.getInt("BlockID"))

      val ship = ShipLocator.getShip(player.worldObj, shipID)

      if (isNullOrEmpty(ship, "BlockActionHandler", shipID)) return
      ship.get.Shipworld.addBlockEvent(blockPos, block, data1, data2)
    }
  }

  object BlockChangedHandler extends BiConsumer[EntityPlayer,Token] {
    override def accept(player: EntityPlayer, token: Token): Unit = {

      val shipID = token.getInt("ShipID")
      val blockPos = BlockPos.fromLong(token.getLong("BlockPosition"))
      val blockstate = Block.getStateById(token.getInt("Blockstate"))
      val hastileentity = token.getBoolean("HasTileEntity")
      val tileEntity = if (hastileentity) Some(token.getNBT("TileEntity")) else None


      def player = Minecraft.getMinecraft.thePlayer

      val Ship = ShipLocator.getShip(player.worldObj, shipID)

      if (isNullOrEmpty(Ship, "BlockChangedHandler", shipID)) return

      Ship.get.Shipworld.setBlockState(blockPos, blockstate, 3)

      if (hastileentity && tileEntity.isDefined) {
        val teOnShip = Ship.get.Shipworld.getTileEntity(blockPos)
        if (teOnShip != null)
          teOnShip.readFromNBT(tileEntity.get)
        else {
          val te = TileEntity.createAndLoadEntity(tileEntity.get)
          te.setWorldObj(Ship.get.Shipworld)
          te.validate()
          Ship.get.Shipworld.addTileEntity(te)
        }
      }
    }
  }

  object BlockDiggingHandler extends BiConsumer[EntityPlayer,Token] {
    override def accept(p: EntityPlayer, token: Token): Unit = {
      val shipID = token.getInt("ShipID")
      val blockpos = BlockPos fromLong token.getLong("BlockPosition")
      val side = EnumFacing.values()(token.getInt("Side"))
      val status = C07PacketPlayerDigging.Action.values()(token.getInt("Status"))
      val player = p.asInstanceOf[EntityPlayerMP]
      val ship = ShipLocator.getShip(player.worldObj,shipID)
      if (isNullOrEmpty(ship,"BlockDiggingHandler",shipID)) return
      val shipWorld = ship.get.Shipworld

      player.markPlayerActive()

      status match {
        case Action.DROP_ITEM => if (!player.isSpectator) player.dropOneItem(false)

        case Action.DROP_ALL_ITEMS => if (!player.isSpectator) player.dropOneItem(true)

        case Action.RELEASE_USE_ITEM => player.stopUsingItem()

        case Action.START_DESTROY_BLOCK =>
          if (shipWorld.getWorldBorder.contains(blockpos))
            ItemInWorldManagerFaker.onBlockClicked(player, blockpos, side, shipWorld)
          else
            PacketSender.sendBlockChangedPacket(shipWorld,blockpos)

        case Action.ABORT_DESTROY_BLOCK => shipWorld.sendBlockBreakProgress(player.getEntityId,blockpos,-1)

        case Action.STOP_DESTROY_BLOCK =>  ItemInWorldManagerFaker.blockRemoving(player,blockpos,shipWorld)

        case _ => throw new IllegalArgumentException("Invalid player action")
      }

      if (shipWorld.getBlockState(blockpos).getBlock.getMaterial != Material.air)
        PacketSender.sendBlockChangedPacket(shipWorld,blockpos)


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

        // World PlayAuxSFXAtEntity manual
        shipWorld.playAuxSFXAtEntity(player, 2001, pos, Block.getStateId(iblockstate))
        //EffectRendererShip.addBlockDestroyEffects(Shipworld, pos, block.getStateFromMeta(par4 >> 12 & 255))
        // End of manual

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
              val te = shipWorld.getTileEntity(pos)
              val nbt = new NBTTagCompound()
              if (te != null)
                te.writeToNBT(nbt)

              FlyingShips.network.send().packet("BlockChanged")
                .`with`("ShipID", shipWorld.Ship.ShipID)
                .`with`("BlockPosition", pos.toLong())
                .`with`("Blockstate", Block.getStateId(shipWorld.getBlockState(pos)))
                .`with`("HasTileEntity", !nbt.hasNoTags())
                .`with`("TileEntity", nbt)
            }
            f = block.getPlayerRelativeBlockHardness(player, shipWorld, pos)
          }
          if (event.useItem == net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) {
            if (f >= 1.0F) {
              val te = shipWorld.getTileEntity(pos)
              val nbt = new NBTTagCompound()
              if (te != null)
                te.writeToNBT(nbt)

              FlyingShips.network.send().packet("BlockChanged")
                .`with` ("ShipID", shipWorld.Ship.ShipID)
                .`with` ("BlockPosition", pos.toLong())
                .`with` ("Blockstate", Block.getStateId(shipWorld.getBlockState(pos)))
                .`with`("HasTileEntity", !nbt.hasNoTags())
                .`with` ("TileEntity", nbt)
                .to(player)
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
  }

  object BlockPlacedHandler extends BiConsumer[EntityPlayer,Token] {
    override def accept(p: EntityPlayer, token: Token): Unit = {

      val shipID = token.getInt("ShipID")
      val ship = ShipLocator.getShip(p.worldObj,shipID)

      if (isNullOrEmpty(ship, "BlockPlacedHandler", shipID)) return
      val shipWorld = ship.get.Shipworld

      val blockpos = token.getBlockPos("BlockPosition")
      val enumfacing = Try(EnumFacing.values()(token.getInt("Side")))
      val heldItem = ByteBufUtils.readItemStack(Unpooled.wrappedBuffer(token.getData("HeldItem")))
      val hitVec = new Vec3(token.getDouble("HitVecX"),token.getDouble("HitVecY"),token.getDouble("HitVecZ"))
      val player = p.asInstanceOf[EntityPlayerMP]

      var itemstack: ItemStack = player.inventory.getCurrentItem
      var flag: Boolean = false
      var placeResult: Boolean = true
      player.markPlayerActive()


      if (token.getInt("Side") == 255) {
        if (itemstack == null) {
          return
        }
        val event: PlayerInteractEvent = net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(player, net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_AIR, shipWorld, new BlockPos(0, 0, 0), null)
        if (event.useItem != net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) {
          player.theItemInWorldManager.tryUseItem(player, shipWorld, itemstack)
        }
      }
      else {
        var dist: Double = player.theItemInWorldManager.getBlockReachDistance + 3
        dist *= dist
        if (shipWorld.getWorldBorder.contains(blockpos)) {
          placeResult = player.theItemInWorldManager.activateBlockOrUseItem(player, shipWorld, itemstack, blockpos, enumfacing.get, hitVec.xCoord.toFloat, hitVec.yCoord.toFloat, hitVec.zCoord.toFloat)
        }
        flag = true
      }


      if (flag) {
        // Fire off some packets
        PacketSender.sendBlockChangedPacket(shipWorld,blockpos).to(player)
        PacketSender.sendBlockChangedPacket(shipWorld,blockpos.offset(enumfacing.get)).to(player)
      }

      itemstack = player.inventory.getCurrentItem

      if (itemstack != null && itemstack.stackSize == 0) {
        player.inventory.mainInventory(player.inventory.currentItem) = null
        itemstack = null
      }

      if (itemstack == null || itemstack.getMaxItemUseDuration == 0) {
        player.isChangingQuantityOnly = true
        player.inventory.mainInventory(player.inventory.currentItem) = ItemStack.copyItemStack(player.inventory.mainInventory(player.inventory.currentItem))
        var slot: Slot = player.openContainer.getSlotFromInventory(player.inventory, player.inventory.currentItem)
        var windowId: Int = player.openContainer.windowId
        if (slot == null) {
          slot = player.inventoryContainer.getSlotFromInventory(player.inventory, player.inventory.currentItem)
          windowId = player.inventoryContainer.windowId
        }
        player.openContainer.detectAndSendChanges()
        player.isChangingQuantityOnly = false
        if (!ItemStack.areItemStacksEqual(player.inventory.getCurrentItem, heldItem) || !placeResult) {
          player.playerNetServerHandler.sendPacket(new S2FPacketSetSlot(windowId, slot.slotNumber, player.inventory.getCurrentItem))
        }
      }
    }
  }

  object ChunkDataHandler extends BiConsumer[EntityPlayer,Token] {
    override def accept(player: EntityPlayer, token: Token): Unit = {
      println("got chunk")
      val shipID = token.getInt("ShipID")
      val chunkX = token.getInt("ChunkX")
      val chunkZ = token.getInt("ChunkZ")
      val chunkData = token.getData("ChunkData")
      val par2 = token.getBoolean("Par2")

      val ship = ShipLocator.getClientShip(shipID)
      if (isNullOrEmpty(ship, "ChunkDataHandler", shipID)) return

      val chunk = ship.get.Shipworld.getChunkFromChunkCoords(chunkX, chunkZ)
      chunk.fillChunk(chunkData, chunkData.length, par2)

    }
  }

  object DeleteShipHandler extends BiConsumer[EntityPlayer,Token] {
    override def accept(player: EntityPlayer, token: Token): Unit = {
      val shipID = token.getInt("ShipID")
      val ship = ShipLocator.getClientShip(shipID)
      if (ship.isEmpty)
        FlyingShips.logger.warn(s"DeleteShipMessageHandler: Could not find Ship ID ${shipID} on client, cannot delete it!")
      else
        EntityShipTracker.removeShipClientSide(ship.get)
    }
  }

  object MultipleBlocksChangedHandler extends BiConsumer[EntityPlayer,Token] {
    override def accept(player: EntityPlayer, token: Token): Unit = {
      println("got changedblocks")
      val shipID = token.getInt("ShipID")
      val ship = ShipLocator.getClientShip(shipID)

      if (isNullOrEmpty(ship, "MultipleBlocksChangedHandler", shipID)) return

      val chunkCoords = new ChunkCoordIntPair(token.getInt("ChunkX"),token.getInt("ChunkZ"))

      val changedBlocksData = token.getData("ChangedBlocks")
      val byteBuf = Unpooled.wrappedBuffer(changedBlocksData)
      val changedBlocks = (0 until changedBlocksData.length / 6).map(i => new BlockUpdateData(byteBuf.readShort(),Block.getStateById(byteBuf.readInt())))

      changedBlocks.foreach(block => ship.get.Shipworld.setBlockState(getPos(block.getChunkPosCrammed,chunkCoords), block.getBlockState, 3))


    }
    def getPos(cpc: Short, chunkCoords:ChunkCoordIntPair): BlockPos = {
      chunkCoords.getBlock(cpc >> 12 & 15, cpc & 255, cpc >> 8 & 15)
    }
    case class BlockUpdateData(chunkPosCrammed: Short, blockState: IBlockState) {

      def this(par1: Short, chunkIn: Chunk) = this(par1, chunkIn.getBlockState(getPos(par1,new ChunkCoordIntPair(chunkIn.xPosition,chunkIn.zPosition))))

      def getChunkPosCrammed: Short = {
        this.chunkPosCrammed
      }

      def getBlockState: IBlockState = {
        this.blockState
      }
    }
  }

  object ShipMovementHandler extends BiConsumer[EntityPlayer,Token] {
    // Gets the position, rotation, and whether it was a relative movement (all optional)
    def parseData(byteData: Array[Byte]):(Option[Vec3i],Option[(Int,Int,Int,Int)],Boolean) = {

      val byteBuf = Unpooled.wrappedBuffer(byteData)

      byteData.length match {
        // Only position update was sent (PosX, PosY, PosZ are all integers which are 4 bytes)
        case 12 => (Some(new Vec3i(byteBuf.readInt(), byteBuf.readInt(), byteBuf.readInt())),None,false)
        // Only position update and relative flag was sent (Relative flag is a boolean which is 1 byte)
        case 13 => (Some(new Vec3i(byteBuf.readInt(), byteBuf.readInt(), byteBuf.readInt())),None,true)
        // Only rotation data was sent (Rot.X, Rot.Y, Rot.Z, Rot.W are all integers which are 4 bytes)
        case 16 => (None,Some((byteBuf.readInt(), byteBuf.readInt(), byteBuf.readInt(), byteBuf.readInt())),false)
        // Position update and Rotation was sent
        case 28 => (Some(new Vec3i(byteBuf.readInt(), byteBuf.readInt(), byteBuf.readInt())),Some((byteBuf.readInt(), byteBuf.readInt(), byteBuf.readInt(), byteBuf.readInt())),false)
        // Position update, Rotation, and relative movement was sent
        case 29 =>  (Some(new Vec3i(byteBuf.readInt(), byteBuf.readInt(), byteBuf.readInt())),Some((byteBuf.readInt(), byteBuf.readInt(), byteBuf.readInt(), byteBuf.readInt())),true)
        // Something screwed up sending the data
        case _ => (None,None,false)
      }


    }

    override def accept(player: EntityPlayer, token: Token): Unit = {
      def unencodePos(x:Int): Double = x / 32d
      def unencodeRot(x:Int): Float = x / 1000000f

      val shipID = token.getInt("ShipID")
      val ship = ShipLocator.getShip(player.worldObj,shipID)

      if (isNullOrEmpty(ship,"ShipMovementHandler",shipID)) return

      val data = parseData(token.getData("MovementData"))

      if (data._1.isEmpty && data._2.isEmpty) {
        FlyingShips.logger.warn(s"ShipMovementHandler: Received invalid packet for Ship ID: $shipID! Ignoring...")
        return
      }

      val pos = data._1
      val rot = data._2
      val isRelativeMove = data._3

      if (data._1.isDefined) {
        if (isRelativeMove) {
          ship.get.serverPosX += pos.get.getX
          ship.get.serverPosY += pos.get.getY
          ship.get.serverPosZ += pos.get.getZ
        }
        else {
          ship.get.serverPosX = pos.get.getX
          ship.get.serverPosY = pos.get.getY
          ship.get.serverPosZ = pos.get.getZ
        }

        val x = unencodePos(ship.get.serverPosX)
        val y = unencodePos(ship.get.serverPosY)
        val z = unencodePos(ship.get.serverPosZ)

        ship.get.setPosition(x,y,z)
      }

      if (rot.isDefined) {
        val quat4f = new Quat4f(unencodeRot(rot.get._1),unencodeRot(rot.get._2),unencodeRot(rot.get._3),unencodeRot(rot.get._4))
        ship.get.setRotationFromServer(quat4f)
      }



    }
  }

  object UpdateTileEntityHandler extends BiConsumer[EntityPlayer,Token] {
    override def accept(player: EntityPlayer, token: Token): Unit = {
      val shipID  = token.getInt("ShipID")
      val ship = ShipLocator.getClientShip(shipID)
      if (PacketHandlers.isNullOrEmpty(ship, "UpdateTileEntityHandler",shipID)) return

      val blockpos = token.getBlockPos("BlockPosition")
      val metadata = token.getInt("Metadata")
      val NBT = token.getNBT("NBT")


      val shipWorld = ship.get.Shipworld

      if (shipWorld.isBlockLoaded(blockpos)) {
        val tileentity: TileEntity = shipWorld.getTileEntity(blockpos)
        val i: Int = metadata
        if (i == 1 && tileentity.isInstanceOf[TileEntityMobSpawner] || i == 2 && tileentity.isInstanceOf[TileEntityCommandBlock] || i == 3 && tileentity.isInstanceOf[TileEntityBeacon] || i == 4 && tileentity.isInstanceOf[TileEntitySkull] || i == 5 && tileentity.isInstanceOf[TileEntityFlowerPot] || i == 6 && tileentity.isInstanceOf[TileEntityBanner]) {
          tileentity.readFromNBT(NBT)
        }
        else {
          tileentity.onDataPacket(player.asInstanceOf[EntityPlayerMP].playerNetServerHandler.netManager, new S35PacketUpdateTileEntity(blockpos, metadata, NBT))
        }
      }
    }
  }

  object ShipVelocityHandler extends BiConsumer[EntityPlayer,Token] {
    override def accept(player: EntityPlayer, token: Token): Unit = {
      def unencodeInteger(x:Int): Double = x / 8000.0D

      val shipID = token.getInt("ShipID")
      val ship = ShipLocator.getClientShip(shipID)

      if (isNullOrEmpty(ship, "ShipVelocityHandler", shipID)) return

      val motionX = unencodeInteger(token.getInt("MotionX"))
      val motionY = unencodeInteger(token.getInt("MotionY"))
      val motionZ = unencodeInteger(token.getInt("MotionZ"))
      ship.get.setVelocity(motionX, motionY, motionZ)
    }
  }

}
