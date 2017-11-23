package deltas.javac.expressions.relational

import core.deltas.grammars.LanguageGrammars
import core.deltas.node.GrammarKey
import core.deltas.{Contract, DeltaWithGrammar, Language}
import deltas.javac.expressions.ExpressionSkeleton

object AddRelationalPrecedence extends DeltaWithGrammar {

  override def dependencies: Set[Contract] = Set(ExpressionSkeleton)

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    val expressionGrammar = grammars.find(ExpressionSkeleton.ExpressionGrammar)
    val relationalGrammar = grammars.create(RelationalExpressionGrammar, expressionGrammar.inner)
    expressionGrammar.inner = relationalGrammar
  }

  object RelationalExpressionGrammar extends GrammarKey

  override def description: String = "Creates a named grammar with the correct precedence for relational operators."
}