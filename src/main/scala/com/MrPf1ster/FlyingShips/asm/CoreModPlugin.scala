package com.MrPf1ster.FlyingShips.asm

import java.util

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.{MCVersion, Name, TransformerExclusions}

/**
  * Created by EJ on 7/2/2016.
  */

@Name( "com.MrPf1ster.FlyingShips.core" )
@MCVersion("1.8.9")
@TransformerExclusions( Array("com.MrPf1ster.FlyingShips.asm"))
object CoreModPlugin {
  var isObfuscatedEnvironment:Boolean = false // fix
}
class CoreModPlugin extends IFMLLoadingPlugin {



  override def getASMTransformerClass: Array[String] = Array("com.MrPf1ster.FlyingShips.asm.CoreModTransformer")

  override def injectData(data: util.Map[String, AnyRef]): Unit = {
    // If environment is obfuscated
    CoreModPlugin.isObfuscatedEnvironment = data.get( "runtimeDeobfuscationEnabled" ).asInstanceOf[Boolean]
  }

  override def getModContainerClass: String = "com.MrPf1ster.FlyingShips.FlyingShips"

  override def getAccessTransformerClass: String = null

  override def getSetupClass: String = null
}
