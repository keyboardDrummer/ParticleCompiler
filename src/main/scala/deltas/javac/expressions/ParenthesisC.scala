package deltas.javac.expressions

import core.deltas.grammars.LanguageGrammars
import core.deltas.{Language, Contract, DeltaWithGrammar}

object ParenthesisC extends DeltaWithGrammar {

  override def dependencies: Set[Contract] = Set(ExpressionSkeleton)

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val core = find(ExpressionSkeleton.CoreGrammar)
    val expression = find(ExpressionSkeleton.ExpressionGrammar)
    val parseParenthesis = ("(" ~> value("(" )) ~> expression ~< ")" //TODO: the produce("(") here is a hack to make sure no () show up when printing. Nicer option would be to have a BiGrammar that specifies parsing/printing behavior.
    core.addOption(parseParenthesis)
  }

  override def description: String = "Allows wrapping an expression in parenthesis to control operator precedence."
}