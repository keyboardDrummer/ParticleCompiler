package transformations.bytecode.coreInstructions

import core.transformation.{MetaObject, TransformationState}
import transformations.bytecode.ByteCodeSkeleton
import transformations.bytecode.ByteCodeSkeleton._
import transformations.bytecode.PrintByteCode._
import transformations.bytecode.simpleBytecode.ProgramTypeState
import transformations.javac.classes.ConstantPool

object InvokeSpecialC extends InvokeC {
  override val key: AnyRef = InvokeSpecialKey

  def invokeSpecial(location: Int): MetaObject = instruction(InvokeSpecialKey, Seq(location))

  override def getInstructionByteCode(instruction: MetaObject): Seq[Byte] = {
    val arguments = ByteCodeSkeleton.getInstructionArguments(instruction)
    hexToBytes("b7") ++ shortToBytes(arguments(0))
  }

  override def getInstructionInAndOutputs(constantPool: ConstantPool, instruction: MetaObject, typeState: ProgramTypeState, state: TransformationState): InstructionSignature = {
    getInstanceInstructionInAndOutputs(constantPool, instruction, typeState, state)
  }

  object InvokeSpecialKey

}