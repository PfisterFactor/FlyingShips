package mrpf1ster.flyingships.command

import java.util

import mrpf1ster.flyingships.util.ShipLocator
import net.minecraft.command.{ICommand, ICommandSender}
import net.minecraft.util.BlockPos

/**
  * Created by EJPfi on 1/13/2017.
  */
class TeleportShipCommand extends ICommand {

  val aliases = new util.ArrayList[String]()
  aliases.add(getCommandName)
  aliases.add(getCommandName.toLowerCase)

  override def getCommandName: String = "teleportShip"

  override def canCommandSenderUseCommand(sender: ICommandSender): Boolean = sender.canCommandSenderUseCommand(4, getCommandName)

  override def getCommandAliases: util.List[String] = aliases

  override def isUsernameIndex(args: Array[String], index: Int): Boolean = false

  override def getCommandUsage(sender: ICommandSender): String = s"/$getCommandName [ShipID] [X] [Y] [Z]"

  override def addTabCompletionOptions(sender: ICommandSender, args: Array[String], pos: BlockPos): util.List[String] = null

  override def processCommand(sender: ICommandSender, args: Array[String]): Unit = {
    val ship = ShipLocator.getShip(sender.getEntityWorld,args(0).toInt)
    if (ship.isDefined)
      ship.get.setPosition(args(1).toInt,args(2).toInt,args(3).toInt)
  }

  override def compareTo(other: ICommand): Int = getCommandName.compareTo(other.getCommandName)
}

