package deltas.bytecode.extraBooleanInstructions

import core.language.node.{Node, NodeShape}
import core.deltas.Contract
import core.language.Language
import deltas.bytecode.attributes.CodeAttributeDelta
import deltas.bytecode.coreInstructions.integers.SmallIntegerConstantDelta
import deltas.bytecode.coreInstructions.integers.integerCompare.IfIntegerCompareGreaterOrEqualDelta
import deltas.bytecode.simpleBytecode.{InferredStackFrames, LabelDelta, LabelledLocations}

object GreaterThanInstructionDelta extends ExpandInstruction {

  def greaterThanInstruction = CodeAttributeDelta.instruction(GreaterThanInstructionKey)

  override def dependencies: Set[Contract] = super.dependencies ++ Set(LabelledLocations, IfIntegerCompareGreaterOrEqualDelta)

  override val key = GreaterThanInstructionKey

  override def expand(instruction: Node, methodInfo: Node, state: Language): Seq[Node] = {
    val trueLabel = LabelDelta.getUniqueLabel("true", methodInfo)
    val endLabel = LabelDelta.getUniqueLabel("end", methodInfo)
    Seq(LabelledLocations.ifIntegerCompareGreater(trueLabel),
      SmallIntegerConstantDelta.integerConstant(0),
      LabelledLocations.goTo(endLabel),
      InferredStackFrames.label(trueLabel),
      SmallIntegerConstantDelta.integerConstant(1),
      InferredStackFrames.label(endLabel))
  }

  object GreaterThanInstructionKey extends NodeShape

  override def description: String = "Defines a custom instruction which applies > to the top stack values."

  override def grammarName = "igt"
}
