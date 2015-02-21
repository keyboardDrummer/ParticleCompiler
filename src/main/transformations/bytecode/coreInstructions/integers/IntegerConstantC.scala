package transformations.bytecode.coreInstructions.integers

import core.transformation.{MetaObject, TransformationState}
import transformations.bytecode.simpleBytecode.ProgramTypeState
import transformations.bytecode.{PrintByteCode, ByteCodeSkeleton}
import transformations.bytecode.ByteCodeSkeleton._
import PrintByteCode._
import transformations.bytecode.coreInstructions.{InstructionSignature, InstructionC}
import transformations.javac.classes.ConstantPool
import transformations.types.IntTypeC

object IntegerConstantC extends InstructionC {

  override val key: AnyRef = IntegerConstantKey

  def integerConstant(value: Int) = {
    require (value <= 5)
    require (value >= -1)
    instruction(IntegerConstantKey, Seq(value))
  }

  override def getInstructionByteCode(instruction: MetaObject): Seq[Byte] = {
    byteToBytes(3 + ByteCodeSkeleton.getInstructionArguments(instruction)(0))
  }

  override def getInstructionInAndOutputs(constantPool: ConstantPool, instruction: MetaObject, typeState: ProgramTypeState, state: TransformationState): InstructionSignature =
    InstructionSignature(Seq(), Seq(IntTypeC.intType))

  override def getInstructionSize(instruction: MetaObject): Int = 1

  private object IntegerConstantKey

}
