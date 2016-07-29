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
    def workAround: Unit = classNode.methods.foreach(method => {
      if (method.name == targetMethod && method.desc == targetDesc) {

        val iter = method.instructions.iterator()
        var index = -1
        while (iter.hasNext) {
          index += 1
          val node = iter.next()
          if (node.getOpcode == Opcodes.INVOKEVIRTUAL) {
            val castedNode = node.asInstanceOf[MethodInsnNode]
            val previousOpcode = node.getPrevious
            val ldcNode = if (previousOpcode.getOpcode == Opcodes.LDC) previousOpcode.asInstanceOf[LdcInsnNode] else null
            if (castedNode.name == profilerMethodName && previousOpcode != null && ldcNode.cst.isInstanceOf[String] && ldcNode.cst.asInstanceOf[String] == "entities") {
              // Ahh! Long if-statements are scary!
              targetNode = castedNode
              invokevirtualindex = index
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
        // Loads z onth the stack
        insnList.add(new VarInsnNode(Opcodes.DLOAD, 9))
        // Calls our method in our ShipRender object -- onRender
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "mrpf1ster/flyingships/render/ShipRender", "onRender", insertedInstructionParams))
        mv.instructions.insert(targetNode, insnList)

        println("RenderGlobal patched")
        writeClass = true
        return
      }
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
