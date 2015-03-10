package transformations.bytecode.coreInstructions

import core.particles.{CompilationState, MetaObject}
import transformations.bytecode.PrintByteCode._
import transformations.bytecode.attributes.CodeAttribute
import transformations.bytecode.simpleBytecode.ProgramTypeState
import transformations.javac.classes.ConstantPool

object InvokeVirtualC extends InvokeC {

  override val key: AnyRef = InvokeVirtual

  def invokeVirtual(methodRefIndex: Int) = CodeAttribute.instruction(InvokeVirtual, Seq(methodRefIndex))

  override def getInstructionByteCode(instruction: MetaObject): Seq[Byte] = {
    val arguments = CodeAttribute.getInstructionArguments(instruction)
    hexToBytes("b6") ++ shortToBytes(arguments(0))
  }

  override def getInstructionInAndOutputs(constantPool: ConstantPool, instruction: MetaObject, typeState: ProgramTypeState, state: CompilationState): InstructionSignature = {
    getInstanceInstructionInAndOutputs(constantPool, instruction, typeState, state)
  }

  object InvokeVirtual

  override def description: String = "Defines the invoke virtual instruction, which can be used to call virtual methods."
}
