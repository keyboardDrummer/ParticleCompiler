package deltas.bytecode.coreInstructions.objects

import core.bigrammar.BiGrammar
import core.deltas.grammars.LanguageGrammars
import core.deltas.node.{Node, NodeClass, NodeField}
import core.deltas.{Compilation, Language}
import deltas.bytecode.ByteCodeSkeleton
import deltas.bytecode.PrintByteCode._
import deltas.bytecode.constants.FieldRefConstant
import deltas.bytecode.constants.FieldRefConstant.FieldRefWrapper
import deltas.bytecode.coreInstructions.{ConstantPoolIndexGrammar, InstructionDelta, InstructionSignature}
import deltas.bytecode.simpleBytecode.ProgramTypeState

object GetFieldDelta extends InstructionDelta {

  override val key = GetFieldKey
  object FieldRef extends NodeField

  def construct(fieldRefIndex: Any): Node = GetFieldKey.create(FieldRef -> fieldRefIndex)

  override def getInstructionByteCode(instruction: Node): Seq[Byte] = {
    hexToBytes("b4") ++ shortToBytes(instruction(FieldRef).asInstanceOf[Int])
  }

  override def getSignature(instruction: Node, typeState: ProgramTypeState, state: Compilation): InstructionSignature = {
    val stackTop = typeState.stackTypes.last
    assertObjectTypeStackTop(stackTop, "getField")
    new InstructionSignature(Seq(stackTop), Seq(getReturnType(instruction)))
  }

  def getReturnType(getField: Node): Node = {
    val fieldRef: FieldRefWrapper[Node] = getField(FieldRef).asInstanceOf[Node]
    fieldRef.nameAndType._type.value
  }

  override def inject(state: Language): Unit = {
    super.inject(state)
    ByteCodeSkeleton.getRegistry(state).constantReferences.put(key, Map(FieldRef -> FieldRefConstant.key))
  }

  override def argumentsGrammar(grammars: LanguageGrammars): BiGrammar = {
    import grammars._
    find(ConstantPoolIndexGrammar).as(FieldRef)
  }

  override def getInstructionSize: Int = 3

  object GetFieldKey extends NodeClass

  override def grammarName = "getfield"
}