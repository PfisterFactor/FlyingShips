package com.MrPf1ster.FlyingShips.asm

import net.minecraft.launchwrapper.IClassTransformer

/**
  * Created by EJ on 7/2/2016.
  */
object CoreModTransformer {
}
class CoreModTransformer extends IClassTransformer {

  override def transform(name: String, transformedName: String, classData: Array[Byte]): Array[Byte] = {
    if( classData == null )
    {
      throw new Error( "Transformer received no class data for " + name + ":" + transformedName + "! This class probably doesn't exist on the server!")
    }

    try {
      // Classes that we don't touch
      val privilegedPackages = Array( "com.MrPf1ster.ships.", "net.minecraftforge.", "cpw." )

      // Skips these classes
      privilegedPackages.foreach(c => if (name.startsWith(c)) return classData)

      if (CoreModPlugin.isObfuscatedEnvironment == null)
        return classData

      return classData


    } catch {
      case e:Throwable => {
        println(s"Something went wrong transforming $name:$transformedName")
        e.printStackTrace()
        return classData
      }
    }


  }
}
