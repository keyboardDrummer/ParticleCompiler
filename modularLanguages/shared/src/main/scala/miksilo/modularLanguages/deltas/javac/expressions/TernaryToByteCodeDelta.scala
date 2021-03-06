package miksilo.modularLanguages.deltas.javac.expressions

import miksilo.modularLanguages.core.deltas.path.NodePath
import miksilo.languageServer.core.language.Compilation
import miksilo.modularLanguages.core.node.Node
import miksilo.modularLanguages.deltas.bytecode.ByteCodeMethodInfo
import miksilo.modularLanguages.deltas.bytecode.simpleBytecode.{InferredStackFrames, LabelDelta, LabelledLocations}
import miksilo.modularLanguages.deltas.expression.TernaryDelta

object TernaryToByteCodeDelta extends ConvertsToByteCodeDelta {

  override def toByteCode(_ternary: NodePath, compilation: Compilation): Seq[Node] = {
    val condition = TernaryDelta.getCondition(_ternary)
    val truePath = TernaryDelta.trueBranch(_ternary)
    val falsePath = TernaryDelta.falseBranch(_ternary)
    val methodInfo = _ternary.findAncestorShape(ByteCodeMethodInfo.Shape)
    val falseLabelName = LabelDelta.getUniqueLabel("false", methodInfo)
    val falseTarget = InferredStackFrames.label(falseLabelName)
    val conditionalBranch = LabelledLocations.ifZero(falseLabelName)
    val endLabelName = LabelDelta.getUniqueLabel("end", methodInfo)
    val end = InferredStackFrames.label(endLabelName)
    val goToEnd = LabelledLocations.goTo(endLabelName)
    val toInstructions = ToByteCodeSkeleton.getToInstructions(compilation)
    toInstructions(condition) ++
      Seq(conditionalBranch) ++
      toInstructions(truePath) ++
      Seq(goToEnd, falseTarget) ++
      toInstructions(falsePath) ++
      Seq(end)
  }

  override def shape = TernaryDelta.Shape

  override def description = "Converts the ternary expression to bytecode."

  override def dependencies = Set(TernaryDelta, InferredStackFrames, LabelledLocations)
}
