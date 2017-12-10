package deltas.javac.statements

import core.deltas._
import core.deltas.grammars.LanguageGrammars
import core.deltas.node.{Node, NodeClass}
import core.deltas.path.{Path, PathRoot, SequenceElement}
import deltas.bytecode.simpleBytecode.LabelDelta
import deltas.javac.methods.MethodDelta

import scala.collection.mutable

/*
For each while loop containing a continue, as start label is placed before the while loop, and the continue's are translated to go-to statements that target the start label.
 */
object WhileContinueDelta extends DeltaWithPhase with DeltaWithGrammar {

  override def description: String = "Moves the control flow to the start of the while loop."

  override def dependencies: Set[Contract] = super.dependencies ++ Set(WhileLoopDelta)

  def transformProgram(program: Node, compilation: Compilation): Unit = {
    val startLabels = new mutable.HashMap[Path, String]()
    PathRoot(program).visitClass(ContinueKey, path => transformContinue(path, startLabels, compilation))
  }

  def transformContinue(continuePath: Path, startLabels: mutable.Map[Path, String], language: Language): Unit = {
    val containingWhile = continuePath.findAncestorClass(WhileLoopDelta.WhileKey)
    val label = startLabels.getOrElseUpdate(containingWhile, addStartLabel(containingWhile))
    continuePath.replaceWith(JustJavaGoto.goto(label))
  }

  def addStartLabel(whilePath: Path): String = {
    val method = whilePath.findAncestorClass(MethodDelta.Clazz)
    val startLabel = LabelDelta.getUniqueLabel("whileStart", method)
    whilePath.asInstanceOf[SequenceElement].replaceWith(Seq(JustJavaLabel.label(startLabel), whilePath.current))
    startLabel
  }

  override def transformGrammars(grammars: LanguageGrammars, language: Language): Unit = {
    val statementGrammar = grammars.find(StatementSkeleton.StatementGrammar)
    statementGrammar.addOption(new NodeGrammar("continue;", ContinueKey))
  }

  object ContinueKey extends NodeClass
  def continue = new Node(ContinueKey)
}
