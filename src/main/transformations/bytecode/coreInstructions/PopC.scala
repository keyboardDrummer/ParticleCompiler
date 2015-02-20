package transformations.bytecode.coreInstructions

import core.transformation.{MetaObject, TransformationState}
import transformations.bytecode.PrintByteCode
import transformations.javac.classes.ConstantPool

object PopC extends InstructionC {

  object PopKey
  override val key: AnyRef = PopKey

  def pop = new MetaObject(PopKey)

  override def getInstructionByteCode(instruction: MetaObject): Seq[Byte] = {
    PrintByteCode.hexToBytes("57")
  }

  override def getInstructionInAndOutputs(constantPool: ConstantPool, instruction: MetaObject, stackTypes: Seq[MetaObject],
                                          state: TransformationState): InstructionSignature = {
    val input: MetaObject = stackTypes.last
    assertSingleWord(state, input)
    InstructionSignature(Seq(input),Seq())
  }
}
