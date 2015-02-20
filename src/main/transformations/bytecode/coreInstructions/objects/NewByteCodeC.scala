package transformations.bytecode.coreInstructions.objects

import core.transformation.{MetaObject, TransformationState}
import transformations.bytecode.ByteCodeSkeleton._
import transformations.bytecode.coreInstructions.{InstructionSignature, InstructionC}
import transformations.bytecode.{ByteCodeSkeleton, PrintByteCode}
import transformations.javac.classes.{ConstantPool, QualifiedClassName}
import transformations.types.ObjectTypeC

object NewByteCodeC extends InstructionC {

  object NewByteCodeKey
  
  def newInstruction(classRefIndex: Int) = instruction(NewByteCodeKey, Seq(classRefIndex))
  
  override val key: AnyRef = NewByteCodeKey

  override def getInstructionByteCode(instruction: MetaObject): Seq[Byte] = {
    val arguments = getInstructionArguments(instruction)
    val location = arguments(0)
    PrintByteCode.hexToBytes("bb") ++ PrintByteCode.shortToBytes(location)
  }

  override def getInstructionInAndOutputs(constantPool: ConstantPool, instruction: MetaObject, stackTypes: Seq[MetaObject],
                                          state: TransformationState): InstructionSignature = {
    val location = ByteCodeSkeleton.getInstructionArguments(instruction)(0)
    val classRef = constantPool.getValue(location).asInstanceOf[MetaObject]
    val className = constantPool.getValue(ByteCodeSkeleton.getClassRefName(classRef)).asInstanceOf[QualifiedClassName]
    val classType = ObjectTypeC.objectType(className)
    InstructionSignature(Seq.empty, Seq(classType))
  }
}
