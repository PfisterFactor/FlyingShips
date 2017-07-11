package mrpf1ster.flyingships.asm

import javassist._
import javassist.bytecode._
import javassist.expr.{ExprEditor, FieldAccess}

import mrpf1ster.flyingships.FlyingShips
import net.minecraft.launchwrapper.IClassTransformer

import scala.collection.JavaConversions._
import scala.tools.asm.tree._
import scala.tools.asm.{ClassReader, ClassWriter, Opcodes}

/**
  * Created by EJ on 7/28/2016.
  */

// Transforms any classes forge sends our way
// But we only touch the ones we absolutely need to modify
class FlyingShipsASMTransformer extends IClassTransformer {

  // Forge calls this method with a class passed in in byte form
  override def transform(name: String, transformedName: String, basicClass: Array[Byte]): Array[Byte] = {
    // If it's name matches any of these, then we transform them
    // The seemingly random letters are the MCP obfuscated method names for the class under it
    val returnClass = name match {
      case "bfr" => renderGlobalTransformer(basicClass, obfuscated = true)
      case "net.minecraft.client.renderer.RenderGlobal" => renderGlobalTransformer(basicClass, obfuscated = false)
      //case "bfk" => entityrendererTransformer(basicClass, obfuscated = true)
      //case "net.minecraft.client.renderer.EntityRenderer" => entityrendererTransformer(basicClass, obfuscated = false)
      case "bda" => playerControllerMPTransformer(name, basicClass, obfuscated = true)
      case "net.minecraft.client.multiplayer.PlayerControllerMP" => playerControllerMPTransformer(name, basicClass, obfuscated = false)
      case "ave" => minecraftTransformer(name, basicClass, obfuscated = true)
      case "net.minecraft.client.Minecraft" => minecraftTransformer(name, basicClass, obfuscated = false)
      case _ => basicClass
    }
    // Return the class with any modifications made to it
    // We were gentle, hopefully
    returnClass
  }

  // This method transforms Minecraft's RenderGlobal class and adds a hook method, shown like as...
  /*
  ...

  this.theWorld.theProfiler.endStartSection("entities")
  mrpf1ster.flyingships.render.RenderShip.onRender(partialTicks,camera,d0,d1,d2)
  label738:

  for (RenderGlobal.ContainerLocalRenderInformation renderglobal$containerlocalrenderinformation : this.renderInfos)
  {
    Chunk chunk = this.theWorld.getChunkFromBlockCoords(renderglobal$containerlocalrenderinformation.renderChunk.getPosition());

  ...
  */
  // We do this by first being passed the RenderGlobal class by our transform function
  // Then establishing if we're obfuscated and changing our method variables accordingly (MCP Mappings: mcp_stable-22-1.8.9)
  // Then iterating through each method in the class until we find: "this.theWorld.theProfiler.endStartSection("entities")"
  // Then we create a Instruction list of our method intermediary in bytecode
  // Then we insert that list after the method we found
  // Finally we write it all back to bytes and return it
  def renderGlobalTransformer(classBytes: Array[Byte], obfuscated: Boolean): Array[Byte] = {
    FlyingShips.logger.info("Patching RenderGlobal...")
    val classNode = new ClassNode()
    val classReader = new ClassReader(classBytes)
    classReader.accept(classNode, 0)

    val targetMethod = if (obfuscated) "a" else "renderEntities"
    val targetDesc = if (obfuscated) "(Lpk;Lbia;F)V" else "(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V"
    val profilerMethodName = if (obfuscated) "c" else "endStartSection"
    val insertedInstructionParams = if (obfuscated) "(FLbia;DDD)V" else "(FLnet/minecraft/client/renderer/culling/ICamera;DDD)V"

    var methodNode: MethodNode = null
    var targetNode: MethodInsnNode = null
    var writeClass: Boolean = false
    def findNodes(): Unit = classNode.methods
      .filter(method => method.name == targetMethod && method.desc == targetDesc)
      .foreach(method => {
        val iter = method.instructions.iterator()
        while (iter.hasNext) {
          val node = iter.next()
          if (node.getOpcode == Opcodes.INVOKEVIRTUAL) {
            val castedNode = node.asInstanceOf[MethodInsnNode]
            val previousNode = node.getPrevious
            val ldcNode = if (previousNode.getOpcode == Opcodes.LDC) previousNode.asInstanceOf[LdcInsnNode] else null
            if (castedNode.name == profilerMethodName && previousNode != null && ldcNode.cst.isInstanceOf[String] && ldcNode.cst.asInstanceOf[String] == "entities") {
              targetNode = castedNode
              methodNode = method
              return
            }
          }
        }
      })
    findNodes

    if (targetNode != null && methodNode != null) {
      targetNode.accept(methodNode)

      // Our render hook method
      val insnList: InsnList = new InsnList
      // All these lines are basically just method parameters
      // onRender(partialTicks:Float, camera:ICamera, x:Double, y:Double, z:Double)
      // Loads partialticks onto the stack
      insnList.add(new VarInsnNode(Opcodes.FLOAD, 3))
      // Loads camera onto the stack
      insnList.add(new VarInsnNode(Opcodes.ALOAD, 2))
      // Loads x onto the stack
      insnList.add(new VarInsnNode(Opcodes.DLOAD, 5))
      // Loads y onto the stack
      insnList.add(new VarInsnNode(Opcodes.DLOAD, 7))
      // Loads z onto the stack
      insnList.add(new VarInsnNode(Opcodes.DLOAD, 9))
      // Calls our method in our RenderShip object -- onRender
      insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "mrpf1ster/flyingships/render/RenderShip", "onRender", insertedInstructionParams))
      methodNode.instructions.insert(targetNode, insnList)

      FlyingShips.logger.info("RenderGlobal patched.")
      val classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
      classNode.accept(classWriter)
      classWriter.toByteArray
    }
    else
      classBytes
  }

  // Uhh, Todo: add documentation
  // Just read the above
  def entityrendererTransformer(classBytes: Array[Byte], obfuscated: Boolean): Array[Byte] = {
    FlyingShips.logger.info("Patching EntityRenderer...")
    val classNode = new ClassNode()
    val classReader = new ClassReader(classBytes)
    classReader.accept(classNode, 0)

    val targetMethod = if (obfuscated) "a" else "getMouseOver"
    val targetDesc = "(F)V"
    val profilerMethodName = if (obfuscated) "b" else "endSection"

    var writeClass = false

    var methodNode: MethodNode = null
    var targetNode: MethodInsnNode = null
    def findNodes(): Unit = classNode.methods
      .filter(method => method.name == targetMethod && method.desc == targetDesc)
      .foreach(method => {
        val iter = method.instructions.iterator()
        while (iter.hasNext) {
          val node = iter.next()
          if (node.getOpcode == Opcodes.INVOKEVIRTUAL) {
            val castedNode = node.asInstanceOf[MethodInsnNode]
            val prevNode = castedNode.getPrevious
            val castedPrevNode = if (prevNode.getOpcode == Opcodes.GETFIELD) prevNode.asInstanceOf[FieldInsnNode] else null
            if (castedNode.name == profilerMethodName && castedNode.desc == "()V" && castedPrevNode != null && castedPrevNode.name == "mcProfiler") {
              targetNode = castedNode
              methodNode = method
              return
            }
          }
        }
      })
    findNodes
    if (methodNode != null && targetNode != null) {
      targetNode.accept(methodNode)

      val insnList = new InsnList()
      // getMouseOverHook()
      // Calls our method in ShipManager -- onMouseOverHook()
      insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "mrpf1ster/flyingships/ShipManager", "onMouseOverHook", "()V"))
      methodNode.instructions.insert(targetNode, insnList)

      FlyingShips.logger.info("EntityRenderer patched.")
      val classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
      classNode.accept(classWriter)
      classWriter.toByteArray
    }
    else
      classBytes
  }

  // Given an constant pool index, return the resulting bytes that were used to make it
  def getBytesFromIndex(int: Int): (Int, Int) = (int / 256, int % 256)

  // The opposite of above: given two bytes, combine them into a constant pool index
  def getIndexFromBytes(byte1: Int, byte2: Int): Int = (byte1 << 8) + byte2


  // Modifies playerControllerMP and replaces every field access to "theWorld" with our static method hook.
  // This hook determines if the block being modified is on a ship or not, and returns the appropriate world that block is in
  def playerControllerMPTransformer(name: String, classBytes: Array[Byte], obfuscated: Boolean): Array[Byte] = {
    FlyingShips.logger.info(s"Patching PlayerControllerMP...")
    val classBytePath = new ByteArrayClassPath(name, classBytes)
    val classPool = ClassPool.getDefault
    classPool.insertClassPath(classBytePath)
    val ctClass = classPool.get(name)

    val playerControllerReplacerMethods = Array("clickBlockCreative", "onPlayerDestroyBlock", "clickBlock", "resetBlockRemoving", "onPlayerDamageBlock", "onPlayerRightClick")

    ctClass.getClassFile.getMethods.filter(m => playerControllerReplacerMethods.exists(m.asInstanceOf[MethodInfo].getName.contains(_))).foreach(method => {
      val casted = method.asInstanceOf[MethodInfo]
      val constPool = casted.getConstPool

      // Generate some bytecode that contains to our hook method
      // We insert this in place of a call to Minecraft.theWorld
      // Lazy because we don't need to make this if no method gets transformed. Which probably means something went wrong
      val replacingBytecode = new Bytecode(constPool)

      replacingBytecode.addInvokestatic("mrpf1ster/flyingships/ShipManager", "theWorldHook", "()Lnet/minecraft/world/World;")

      val worldClassIndex: Int = constPool.addClassInfo("net/minecraft/world/World")

      val methodRefReplaceMap: Map[String, Int] = Map(
        ("getBlockState", constPool.addMethodrefInfo(worldClassIndex, "getBlockState", "(Lnet/minecraft/util/BlockPos;)Lnet/minecraft/block/state/IBlockState;")),
        ("extinguishFire", constPool.addMethodrefInfo(worldClassIndex, "extinguishFire", "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/BlockPos;Lnet/minecraft/util/EnumFacing;)Z")),
        ("getWorldBorder", constPool.addMethodrefInfo(worldClassIndex, "getWorldBorder", "()Lnet/minecraft/world/border/WorldBorder;")),
        ("sendBlockBreakProgress", constPool.addMethodrefInfo(worldClassIndex, "sendBlockBreakProgress", "(ILnet/minecraft/util/BlockPos;I)V"))
      )
      //println(casted.getName)

      val iterator = casted.getCodeAttribute.iterator()
      // Begin to go through the method's bytecode
      iterator.begin()
      while (iterator.hasNext) {
        // Get the byte index and the opcode at that index
        val index = iterator.next()
        val op = iterator.byteAt(index)
        //println(index + " " + Mnemonic.OPCODE(op))
        // Now we need to replace any method call on "theWorld", which is a WorldClient, with one in the World class, which our world hook returns
        op match {
          case Opcodes.GETFIELD =>
            // We get the constant pool index, of the field its referencing. How we do this is detailed on:
            // https://en.wikipedia.org/wiki/Java_bytecode_instruction_listings#getfield
            val constPoolIndex = getIndexFromBytes(iterator.byteAt(index + 1), iterator.byteAt(index + 2))
            // From this index we get the name of the field that is being referenced
            val fieldName: String = constPool.getFieldrefName(constPoolIndex)
            // If the field contains "theWorld" then we need to replace that with a call to our hook method
            if (fieldName.contains("theWorld")) {
              // Write our replacing bytecode over the GETFIELD opcode
              iterator.write(replacingBytecode.get(), index)

              // Now we go back to find if there's anything on the stack that we need to pop off
              if (index - 1 >= 0 && javassist.bytecode.Opcode.ALOAD_0 == iterator.byteAt(index - 1))
                iterator.writeByte(Opcode.NOP, index - 1)

              if (index - 3 >= 0 && javassist.bytecode.Opcode.GETFIELD == iterator.byteAt(index - 3)) {
                iterator.insertAt(index, Array(Opcode.POP.toByte))
              }

            }
          case Opcode.INVOKEVIRTUAL =>
            val constPoolIndex = getIndexFromBytes(iterator.byteAt(index + 1), iterator.byteAt(index + 2))
            //println("!---!")
            val replaceMethodIndex = methodRefReplaceMap.get(constPool.getMethodrefName(constPoolIndex))
            if (replaceMethodIndex.isDefined) {
              val bytes = getBytesFromIndex(replaceMethodIndex.get)
              iterator.writeByte(bytes._1, index + 1)
              iterator.writeByte(bytes._2, index + 2)
            }
          case _ => Unit
        }


      }

      /*
      println("@@@@@")

      iterator.begin()
      while (iterator.hasNext) {
        val index = iterator.next()
        val op = iterator.byteAt(index)
        println(index + " " + Mnemonic.OPCODE(op))
      }
      */
      casted.rebuildStackMap(classPool)
      if (casted.getName.contains("onPlayerRightClick"))
        casted.setDescriptor(casted.getDescriptor.replace("Lnet/minecraft/client/multiplayer/WorldClient;", "Lnet/minecraft/world/World;"))

    })



    ctClass.rebuildClassFile()
    FlyingShips.logger.info(s"PlayerControllerMP patched.")
    ctClass.toBytecode

  }

  def minecraftTransformer(name: String, classBytes: Array[Byte], obfuscated: Boolean): Array[Byte] = {
    // We can't log using our mod logger when minecraft hasn't even loaded the mod yet
    println("FlyingShips: Patching Minecraft...")
    val classBytePath = new ByteArrayClassPath(name, classBytes)
    val classPool = ClassPool.getDefault
    classPool.insertClassPath(classBytePath)
    val ctClass = classPool.get(name)

    val theWorldReplacerMethods = Array("sendClickBlockToController", "clickMouse", "rightClickMouse", "middleClickMouse")

    ctClass.getClassFile.getMethods.filter(m => theWorldReplacerMethods.exists(m.asInstanceOf[MethodInfo].getName.contains(_))).foreach(method => {

      val casted = method.asInstanceOf[MethodInfo]

      val constPool = casted.getConstPool
      // Generate some bytecode that contains to our hook method
      // We insert this in place of a call to Minecraft.theWorld
      // Lazy because we don't need to make this if no method gets transformed. Which probably means something went wrong
      val replacingBytecode = new Bytecode(constPool)

      replacingBytecode.addInvokestatic("mrpf1ster/flyingships/ShipManager", "theWorldHook", "()Lnet/minecraft/world/World;")


      val worldClassIndex: Int = constPool.addClassInfo("net/minecraft/world/World")
      val playerControllerMPClassIndex: Int = constPool.addClassInfo("net/minecraft/client/multiplayer/PlayerControllerMP")

      val methodRefReplaceMap: Map[String, Int] = Map(
        ("getBlockState", constPool.addMethodrefInfo(worldClassIndex, "getBlockState", "(Lnet/minecraft/util/BlockPos;)Lnet/minecraft/block/state/IBlockState;")),
        ("isAirBlock", constPool.addMethodrefInfo(worldClassIndex, "isAirBlock", "(Lnet/minecraft/util/BlockPos;)Z")),
        ("onPlayerRightClick", constPool.addMethodrefInfo(playerControllerMPClassIndex, "onPlayerRightClick", "(Lnet/minecraft/client/entity/EntityPlayerSP;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/BlockPos;Lnet/minecraft/util/EnumFacing;Lnet/minecraft/util/Vec3;)Z"))
      )

      //println(casted.getName)

      val iterator = casted.getCodeAttribute.iterator()
      // Begin to go through the method's bytecode
      iterator.begin()
      while (iterator.hasNext) {
        // Get the byte index and the opcode at that index
        val index = iterator.next()
        val op = iterator.byteAt(index)
        //println(index + " " + Mnemonic.OPCODE(op))
        // Now we need to replace any method call on "theWorld", which is a WorldClient, with one in the World class, which our world hook returns
        op match {
          case Opcodes.GETFIELD =>
            // We get the constant pool index, of the field its referencing. How we do this is detailed on:
            // https://en.wikipedia.org/wiki/Java_bytecode_instruction_listings#getfield
            val constPoolIndex = getIndexFromBytes(iterator.byteAt(index + 1), iterator.byteAt(index + 2))
            // From this index we get the name of the field that is being referenced
            val fieldName: String = constPool.getFieldrefName(constPoolIndex)
            // If the field contains "theWorld" then we need to replace that with a call to our hook method
            if (fieldName.contains("theWorld")) {
              // Write our replacing bytecode over the GETFIELD opcode
              iterator.write(replacingBytecode.get(), index)

              // Now we go back to find if there's anything on the stack that we need to pop off
              if (index - 1 >= 0 && javassist.bytecode.Opcode.ALOAD_0 == iterator.byteAt(index - 1))
                iterator.writeByte(Opcode.NOP, index - 1)
            }
          case Opcode.INVOKEVIRTUAL =>
            val constPoolIndex = getIndexFromBytes(iterator.byteAt(index + 1), iterator.byteAt(index + 2))
            //println("!---!")
            val replaceMethodIndex = methodRefReplaceMap.get(constPool.getMethodrefName(constPoolIndex))
            if (replaceMethodIndex.isDefined) {
              val bytes = getBytesFromIndex(replaceMethodIndex.get)
              iterator.writeByte(bytes._1, index + 1)
              iterator.writeByte(bytes._2, index + 2)
            }
          case _ => Unit
        }


      }
      /*
      println("@@@@@")
      iterator.begin()
      while (iterator.hasNext) {
        val index = iterator.next()
        val op = iterator.byteAt(index)
        println(index + " " + Mnemonic.OPCODE(op))
      }
      */
      casted.rebuildStackMap(classPool)
    })

    ctClass.getDeclaredMethods.foreach(method => {
      method.instrument(
        new ExprEditor() {
          @throws[CannotCompileException]
          override def edit(field: FieldAccess): Unit = {
            if (field.isReader && field.getFieldName.contains("objectMouseOver"))
              field.replace("{$_ = mrpf1ster.flyingships.ShipManager.onMouseOverHookExpr();}")

          }
        })
    })
    ctClass.rebuildClassFile()
    println("FlyingShips: Minecraft Patched")
    ctClass.toBytecode

  }

}
