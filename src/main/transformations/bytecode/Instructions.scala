package transformations.bytecode

import core.transformation.{TransformationState, MetaObject}
import transformations.bytecode.LabelledTargets.LabelKey
import transformations.javac.classes.ConstantPool

object Instructions {

  def getInstructionInputTypes(constantPool: ConstantPool, instruction: MetaObject, state: TransformationState): Seq[MetaObject] =
    getInstructionInAndOutputs(constantPool, instruction, state)._1

  def getInstructionInAndOutputs(constantPool: ConstantPool, instruction: MetaObject, state: TransformationState): (Seq[MetaObject], Seq[MetaObject]) =
    instruction.clazz match {
      case LabelKey => (Seq(), Seq())
    }

  def getInstructionOutputTypes(constantPool: ConstantPool, instruction: MetaObject, state: TransformationState): Seq[MetaObject] =
    getInstructionInAndOutputs(constantPool, instruction, state)._2
}
