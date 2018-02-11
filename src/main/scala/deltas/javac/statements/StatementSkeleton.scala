package deltas.javac.statements

import core.deltas._
import core.deltas.grammars.LanguageGrammars
import core.language.node.{GrammarKey, Node, NodeLike, NodeWrapper}
import core.deltas.path.{ChildPath, NodePath}
import core.language.{Compilation, Language}
import core.smarts.ConstraintBuilder
import core.smarts.scopes.objects.Scope
import deltas.javac.expressions.ExpressionSkeleton

object StatementSkeleton extends DeltaWithGrammar {

  implicit class Statement[T <: NodeLike](val node: T) extends NodeWrapper[T] { }

  override def dependencies: Set[Contract] = Set(ExpressionSkeleton)

  def getToInstructions(compilation: Compilation): NodePath => Seq[Node] = {
    statement => getInstance(compilation, statement).toByteCode(statement, compilation)
  }

  def getInstance(compilation: Compilation, statement: NodePath): StatementInstance = {
    instances.get(compilation, statement.shape)
  }

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit =  {
    grammars.create(StatementGrammar)
  }

  object StatementGrammar extends GrammarKey

  override def description: String = "Defines the concept of a statement."

  val instances = new ShapeProperty[StatementInstance]

  def constraints(compilation: Compilation, builder: ConstraintBuilder, statement: ChildPath, parentScope: Scope): Unit = {
    getInstance(compilation, statement).constraints(compilation, builder, statement, parentScope)
  }
}
