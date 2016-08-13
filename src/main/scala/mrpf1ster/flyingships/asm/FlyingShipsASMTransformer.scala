package mrpf1ster.flyingships.asm

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
// ...Promise
class FlyingShipsASMTransformer extends IClassTransformer {

  // Forge calls this method with a class passed in in byte form
  override def transform(name: String, transformedName: String, basicClass: Array[Byte]): Array[Byte] = {
    var returnClass = basicClass
    // If it's name matches any of these, then we transform them
    // The seemingly random letters are the MCP obfuscated method names for the class under it
    name match {
      case "bfr" => returnClass = renderGlobalTransformer(basicClass, obfuscated = true)
      case "net.minecraft.client.renderer.RenderGlobal" => returnClass = renderGlobalTransformer(basicClass, obfuscated = false)
      case "bfk" => returnClass = entityrendererTransformer(basicClass, obfuscated = true)
      case "net.minecraft.client.renderer.EntityRenderer" => returnClass = entityrendererTransformer(basicClass, obfuscated = false)
      case _ =>
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
  // Just read the above and like, use intuition I guess
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

}
