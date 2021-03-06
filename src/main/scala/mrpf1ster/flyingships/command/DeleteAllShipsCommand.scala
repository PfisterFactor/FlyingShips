package mrpf1ster.flyingships.command

import java.util

import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.command.{CommandBase, ICommand, ICommandSender}
import net.minecraft.util.BlockPos

/**
  * Created by EJ on 7/28/2016.
  */

// Useful command for debugging
// Deletes all ships within the sender's world
class DeleteAllShipsCommand extends ICommand {

  val aliases = new util.ArrayList[String]()
  aliases.add(getCommandName)
  aliases.add(getCommandName.toLowerCase)

  override def getCommandName: String = "deleteShips"

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = sender.canCommandSenderUseCommand(4, getCommandName)

  override def getCommandAliases: util.List[String] = aliases

  override def isUsernameIndex(args: Array[String], index: Int): Boolean = false

  override def getCommandUsage(sender: ICommandSender): String = "/" + getCommandName

  override def addTabCompletionOptions(sender: ICommandSender, args: Array[String], pos: BlockPos): util.List[String] = null

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
    ShipLocator.getShips(sender.getEntityWorld).foreach(ship => ship.setDead())
    CommandBase.notifyOperators(sender,this,"All Ships Deleted")

  }

  override def compareTo(other: ICommand): Int = getCommandName.compareTo(other.getCommandName)
}
