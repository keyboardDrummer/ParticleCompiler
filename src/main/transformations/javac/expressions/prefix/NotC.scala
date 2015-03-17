package transformations.javac.expressions.prefix

import core.particles.grammars.GrammarCatalogue
import core.particles.{MetaObjectWithOrigin, CompilationState, MetaObject}
import transformations.bytecode.extraBooleanInstructions.NotInstructionC
import transformations.javac.expressions.{ExpressionInstance, ExpressionSkeleton}
import transformations.types.BooleanTypeC

object NotC extends ExpressionInstance {

  object NotKey

  object NotExpression

  override val key: AnyRef = NotKey

  override def getType(expression: MetaObjectWithOrigin, state: CompilationState): MetaObject = BooleanTypeC.booleanType

  override def toByteCode(expression: MetaObjectWithOrigin, state: CompilationState): Seq[MetaObject] = {
    ExpressionSkeleton.getToInstructions(state)(expression(NotExpression).asInstanceOf[MetaObjectWithOrigin]) ++ Seq(NotInstructionC.not)
  }

  override def transformGrammars(grammars: GrammarCatalogue): Unit = {
    val coreGrammar = grammars.find(ExpressionSkeleton.CoreGrammar)
    coreGrammar.addOption("!" ~> coreGrammar ^^ parseMap(NotKey, NotExpression))
  }

  override def description: String = "Adds the ! (not) operator."
}
