package core.bigrammar.grammars

import core.bigrammar.BiGrammarToParser._
import core.bigrammar.printer.{Printer, TryState}
import core.bigrammar.{BiGrammar, BiGrammarToParser, WithMap}
import core.document.Empty
import core.responsiveDocument.ResponsiveDocument

import scala.util.matching.Regex

object ParseWhiteSpace extends CustomGrammarWithoutChildren[WithMap[Unit]] with BiGrammarWithoutChildren[WithMap[Unit]] {
  val regex: Regex = """\s+""".r

  override def getParserBuilder(keywords: scala.collection.Set[String]): Self[Any] =
    parseRegex(regex, "whitespace", score = -0.001,
      penaltyOption = None, // Do not allow insertion
      allowDrop = false)

  override def write(from: WithMap[Unit]): TryState[ResponsiveDocument] =
    if (regex.replaceSomeIn(from.value.asInstanceOf[String], _ => Some("")).isEmpty) TryState.value(Empty)
    else Printer.fail(s"String ${from.value} was not whitespace")

  override def containsParser(recursive: BiGrammar[_] => Boolean): Boolean = true
}
