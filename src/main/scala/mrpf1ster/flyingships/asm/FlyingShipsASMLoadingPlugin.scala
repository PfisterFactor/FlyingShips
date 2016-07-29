package mrpf1ster.flyingships.asm

import java.util

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin

/**
  * Created by EJ on 7/28/2016.
  */

@IFMLLoadingPlugin.Name("mrpf1ster.flyingships.core")
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.TransformerExclusions(Array[String]("mrpf1ster.flyingships.asm", "scala"))
class FlyingShipsASMLoadingPlugin extends IFMLLoadingPlugin {
  var injectedData: util.Map[String, AnyRef] = null

  override def getASMTransformerClass: Array[String] = Array[String](classOf[FlyingShipsASMTransformer].getName)

  override def injectData(data: util.Map[String, AnyRef]): Unit = injectedData = data

  override def getModContainerClass: String = classOf[CoreModContainer].getName

  override def getAccessTransformerClass: String = null

  override def getSetupClass: String = null
}
