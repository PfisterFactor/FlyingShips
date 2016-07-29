package mrpf1ster.flyingships.asm

import java.util

import mrpf1ster.flyingships.FlyingShips
import net.minecraftforge.fml.common.{DummyModContainer, ModMetadata}

/**
  * Created by EJ on 7/28/2016.
  */
class CoreModContainer extends DummyModContainer(new ModMetadata) {
  val meta = getMetadata
  val authorList = new util.ArrayList[String]()
  authorList.add("MrPf1ster")

  meta.modId = "flyingshipscore"
  meta.name = "Flying Ships Coremod"
  meta.version = FlyingShips.VERSION
  meta.credits = ""
  meta.authorList = authorList
  meta.description = "Contains coremodifications for FlyingShips"

}
