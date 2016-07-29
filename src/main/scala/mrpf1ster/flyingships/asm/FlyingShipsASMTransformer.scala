package mrpf1ster.flyingships.asm

import net.minecraft.launchwrapper.IClassTransformer

import scala.collection.JavaConversions._
import scala.tools.asm.tree._
import scala.tools.asm.{ClassReader, ClassWriter, Opcodes}

/**
  * Created by EJ on 7/28/2016.
  */
class FlyingShipsASMTransformer extends IClassTransformer {

  override def transform(name: String, transformedName: String, basicClass: Array[Byte]): Array[Byte] = {
    var returnClass = basicClass
    name match {
      case "bfr" => returnClass = renderGlobalTransformer(basicClass, true)
      case "net.minecraft.client.renderer.RenderGlobal" => returnClass = renderGlobalTransformer(basicClass, false)
      case _ =>
    }
    returnClass
  }

  // This method transforms Minecraft's RenderGlobal class and adds a hook method, shown like as...
  /*
  ...

  this.theWorld.theProfiler.endStartSection("entities")
  mrpf1ster.flyingships.render.ShipRender.onRender(partialTicks,camera,d0,d1,d2)
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
  def renderGlobalTransformer(classBytes: Array[Byte], obfusucated: Boolean): Array[Byte] = {
    println("Transforming RenderGlobal")
    val classNode = new ClassNode()
    val classReader = new ClassReader(classBytes)
    classReader.accept(classNode, 0)

    val targetMethod = if (obfusucated) "a" else "renderEntities"
    val targetDesc = if (obfusucated) "(Lpk;Lbia;F)V" else "(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V"
    val profilerMethodName = if (obfusucated) "c" else "endStartSection"
    val insertedInstructionParams = if (obfusucated) "(FLbia;DDD)V" else "(FLnet/minecraft/client/renderer/culling/ICamera;DDD)V"
    var invokevirtualindex = -1
    var mv: MethodNode = null
    var targetNode: MethodInsnNode = null
    var writeClass: Boolean = false
    def workAround: Unit = classNode.methods
      .filter(method => method.name == targetMethod && method.desc == targetDesc)
      .foreach(method => {
        val iter = method.instructions.iterator()
        while (iter.hasNext) {
          val node = iter.next()
          if (node.getOpcode == Opcodes.INVOKEVIRTUAL) {
            val castedNode = node.asInstanceOf[MethodInsnNode]
            val previousOpcode = node.getPrevious
            val ldcNode = if (previousOpcode.getOpcode == Opcodes.LDC) previousOpcode.asInstanceOf[LdcInsnNode] else null
            if (castedNode.name == profilerMethodName && previousOpcode != null && ldcNode.cst.isInstanceOf[String] && ldcNode.cst.asInstanceOf[String] == "entities") {
              // Ahh! Long if-statements are scary!
              targetNode = castedNode
              mv = method
            }
          }
        }
        targetNode.accept(mv)

        // Our render method
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
        // Calls our method in our ShipRender object -- onRender
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "mrpf1ster/flyingships/render/ShipRender", "onRender", insertedInstructionParams))
        mv.instructions.insert(targetNode, insnList)

        println("RenderGlobal patched")
        writeClass = true
        return
      })

    workAround
    if (writeClass) {
      val classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
      classNode.accept(classWriter)
      classWriter.toByteArray
    }
    else
      classBytes
  }

}
