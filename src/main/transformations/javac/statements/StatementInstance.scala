package transformations.javac.statements

import core.transformation.sillyCodePieces.GrammarTransformation
import core.transformation.{Contract, MetaObject, TransformationState}

trait StatementInstance extends GrammarTransformation {

  override def inject(state: TransformationState): Unit = {
    StatementC.getStatementToLines(state).put(key, (expression: MetaObject) => toByteCode(expression, state))
  }

  val key: AnyRef

  def toByteCode(statement: MetaObject, state: TransformationState): Seq[MetaObject]

  override def dependencies: Set[Contract] = Set(StatementC) ++ super.dependencies
}