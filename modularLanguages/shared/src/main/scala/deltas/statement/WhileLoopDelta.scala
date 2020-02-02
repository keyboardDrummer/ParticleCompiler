package deltas.statement

import core.deltas._
import core.deltas.grammars.LanguageGrammars
import core.deltas.path.{NodePath, NodeSequenceElement, PathRoot}
import core.language.node._
import core.language.{Compilation, Language}
import deltas.expression.ExpressionDelta

object WhileLoopDelta extends DeltaWithPhase with DeltaWithGrammar {

  override def description: String = "Adds a while loop."

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._

    val statementGrammar = find(StatementDelta.Grammar)
    val expression = find(ExpressionDelta.FirstPrecedenceGrammar)
    val blockGrammar = find(BlockDelta.BlockGrammar)
    val whileGrammar = "while" ~> expression.inParenthesis.as(Condition) %
        blockGrammar.as(Body) asLabelledNode Shape
    statementGrammar.addAlternative(whileGrammar)
  }

  override def transformProgram(program: Node, compilation: Compilation): Unit = {
    PathRoot(program).visitShape(Shape, path => transformWhileLoop(path, compilation))
  }

  def transformWhileLoop(whileLoopPath: NodePath, compilation: Compilation): Unit = {
    val whileLoop: While[Node] = whileLoopPath.current
    val label: String = LabelStatementDelta.getUniqueLabel(compilation, "whileStart", whileLoopPath)
    val startLabel = LabelStatementDelta.neww(label)
    val ifBody = BlockDelta.neww(Seq(whileLoop.body, GotoStatementDelta.neww(label)))
    val _if = IfThenDelta.neww(whileLoop.condition, ifBody)

    val newStatements = Seq[Node](startLabel, _if)
    whileLoopPath.asInstanceOf[NodeSequenceElement].replaceWith(newStatements)
  }

  override def dependencies: Set[Contract] = Set(IfThenDelta, BlockDelta, LabelStatementDelta, GotoStatementDelta)

  implicit class While[T <: NodeLike](val node: T) extends NodeWrapper[T] {
    def condition: T = node(Condition).asInstanceOf[T]
    def body: T = node(Body).asInstanceOf[T]
  }

  def create(condition: Node, body: Node) = new Node(Shape, Condition -> condition, Body -> body)

  object Shape extends NodeShape

  object Condition extends NodeField

  object Body extends NodeField
}