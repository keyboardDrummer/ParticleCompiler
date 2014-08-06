package transformations.bytecode.instructions

import core.transformation.{MetaObject, TransformationState}
import transformations.bytecode.ByteCodeSkeleton._
import transformations.bytecode.PrintByteCode._
import transformations.javac.base.ConstantPool
import transformations.javac.types.IntTypeC

object StoreIntegerC extends InstructionC {

  override val key: Any = IntegerStore

  def integerStore(location: Int) = instruction(IntegerStore, Seq(location))

  override def getInstructionStackSizeModification(constantPool: ConstantPool, instruction: MetaObject, state: TransformationState): Int = -1

  override def getInstructionByteCode(instruction: MetaObject): Seq[Byte] = {
    val arguments = getInstructionArguments(instruction)
    val location = arguments(0)
    if (location > 3)
      hexToBytes("36") ++ byteToBytes(location)
    else
      byteToBytes(hexToInt("3b") + location)
  }

  override def getInstructionInAndOutputs(constantPool: ConstantPool, instruction: MetaObject) = (Seq(IntTypeC.intType), Seq())

  override def getInstructionSize: Int = 2

  object IntegerStore

}
