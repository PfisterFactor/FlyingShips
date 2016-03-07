package com.MrPf1ster.FlyingShips.entities

import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.util.Vec3

/**
  * Created by EJ on 3/6/2016.
  */
class ShipInteractionHandler() {
  def interactionFired(player: EntityPlayer, vec3: Vec3): Boolean = {
    println(vec3)
    true
  }


  def getPlayerReachDistance(player: EntityPlayer): Double = {
    if (player.isInstanceOf[EntityPlayerMP])
      player.asInstanceOf[EntityPlayerMP].theItemInWorldManager.getBlockReachDistance()
    else if (player.isInstanceOf[AbstractClientPlayer])
      Minecraft.getMinecraft.playerController.getBlockReachDistance()
    else
      0
  }
}
