package miksilo.modularLanguages.core.bigrammar.grammars

import miksilo.modularLanguages.core.bigrammar.BiGrammarToParser

import scala.util.matching.Regex
import miksilo.modularLanguages.core.bigrammar.BiGrammarToParser._
import miksilo.editorParser.parsers.editorParsers.History

case class RegexGrammar(regex: Regex, name: String, verifyWhenPrinting: Boolean = false,
                        defaultValue: Option[String] = None,
                        score: Double = History.successValue,
                        penaltyOption: Option[Double] = Some(History.missingInputPenalty),
                        allowDrop: Boolean = true)
  extends StringGrammar(verifyWhenPrinting) {

  override def getParserBuilder(keywords: scala.collection.Set[String]): Parser[Any] =
      BiGrammarToParser.parseRegex(regex, name, defaultValue, score, penaltyOption, allowDrop = allowDrop)
}
