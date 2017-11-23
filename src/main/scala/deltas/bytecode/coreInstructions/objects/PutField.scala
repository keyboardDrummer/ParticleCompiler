package deltas.bytecode.coreInstructions.objects

import core.deltas.grammars.LanguageGrammars
import core.deltas.node.{Node, NodeClass, NodeField}
import core.deltas.{Compilation, Language}
import deltas.bytecode.constants.FieldRefConstant
import deltas.bytecode.coreInstructions.{ByteCodeTypeException, ConstantPoolIndexGrammar, InstructionDelta, InstructionSignature}
import deltas.bytecode.simpleBytecode.ProgramTypeState
import deltas.bytecode.{ByteCodeSkeleton, PrintByteCode}

object PutField extends InstructionDelta {

  object PutFieldKey extends NodeClass
  object FieldRef extends NodeField
  override val key = PutFieldKey

  def putField(index: Any) = PutFieldKey.create(FieldRef -> index)

  override def getInstructionSize: Int = 3
  override def getInstructionByteCode(instruction: Node): Seq[Byte] = {
    PrintByteCode.hexToBytes("b5") ++ PrintByteCode.shortToBytes(instruction(FieldRef).asInstanceOf[Int])
  }

  override def getSignature(instruction: Node, typeState: ProgramTypeState, state: Compilation): InstructionSignature = {
    val stackTop = typeState.stackTypes.takeRight(2)

    if (stackTop.size != 2)
      throw new ByteCodeTypeException("PutField requires two arguments on the stack.")

    val valueType = stackTop(1)
    val objectType = stackTop(0)

    assertObjectTypeStackTop(objectType, "PutField")

    new InstructionSignature(Seq.empty, Seq(valueType, objectType))
  }

  override def inject(state: Language): Unit = {
    super.inject(state)
    ByteCodeSkeleton.getRegistry(state).constantReferences.put(key, Map(FieldRef -> FieldRefConstant.key))
  }

  override def argumentsGrammar(grammars: LanguageGrammars) = {
    import grammars._
    find(ConstantPoolIndexGrammar).as(FieldRef)
  }

  override def grammarName = "putfield"
}