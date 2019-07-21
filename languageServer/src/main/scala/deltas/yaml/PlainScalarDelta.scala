package deltas.yaml

import core.bigrammar.BiGrammar
import core.bigrammar.grammars.{BiSequence, Delimiter, RegexGrammar, SequenceBijective}
import core.deltas.DeltaWithGrammar
import core.deltas.grammars.LanguageGrammars
import core.language.Language
import deltas.expression.{ExpressionDelta, StringLiteralDelta}
import deltas.json.JsonStringLiteralDelta

object PlainScalarDelta extends DeltaWithGrammar {
  def flowIndicatorChars = """,\[\]{}"""

  override def transformGrammars(_grammars: LanguageGrammars, language: Language): Unit = {
    val grammars = _grammars
    import grammars._

    val nonBreakChars = """\n"""
    val nonSpaceChars = """\n """
    val indicatorChars = """-\?:,\[\]\{\}#&*!\|>'"%@`"""
    val allowedInFirst = Set('?',':','-')
    val nonPlainFirstChars = (nonSpaceChars + indicatorChars).filter(c => !allowedInFirst.contains(c))
    val plainSafeOutChars = s"""$nonBreakChars#'"""
    val plainSafeInChars = s"""$plainSafeOutChars$flowIndicatorChars"""
    val doubleColonPlainSafeIn = RegexGrammar(s"""[^$nonPlainFirstChars]([^$plainSafeInChars:]|:[^$plainSafeInChars ])*""".r,
      "plain scalar", defaultValue = Some(""), allowDrop = false)
    val doubleColonPlainSafeOut = RegexGrammar(s"""[^$nonPlainFirstChars]([^$plainSafeOutChars:]|:[^$plainSafeOutChars ])*""".r,
      "plain scalar", defaultValue = Some(""), allowDrop = false)

    val nsPlainSafe: BiGrammar = new IfContext(Map(
      FlowIn -> doubleColonPlainSafeIn,
      FlowOut -> doubleColonPlainSafeOut,
      BlockKey -> doubleColonPlainSafeOut,
      FlowKey -> doubleColonPlainSafeIn), doubleColonPlainSafeOut)

    val plainStyleSingleLineString: BiGrammar = nsPlainSafe
    val plainStyleMultiLineString: BiGrammar = {
      val lineSeparator = new BiSequence(Delimiter("\n", allowDrop = false), _grammars.trivia, BiSequence.ignoreLeft, true)
      val firstLine = new BiSequence(nsPlainSafe, lineSeparator, BiSequence.ignoreRight, false)
      val followingLine = CheckIndentationGrammar.equal(nsPlainSafe)
      val otherLines = CheckIndentationGrammar.greaterThan(new WithIndentationGrammar(followingLine.someSeparated(lineSeparator)))

      new WithIndentationGrammar(new BiSequence(firstLine, otherLines,
        SequenceBijective((firstLine: Any, rest: Any) => {
           rest.asInstanceOf[List[String]].fold(firstLine.asInstanceOf[String])((a, b) => a + " " + b)
        }, (value: Any) => Some(value, List.empty)), false))
    }

    val plainScalar: BiGrammar = new WithContext({
      case FlowIn => FlowIn
      case BlockKey => BlockKey
      case FlowKey => FlowKey
      case _ => FlowOut
    }, plainStyleMultiLineString | plainStyleSingleLineString).
      as(JsonStringLiteralDelta.Value).asLabelledNode(StringLiteralDelta.Shape)

    find(ExpressionDelta.FirstPrecedenceGrammar).addAlternative(plainScalar)

  }

  override def description = "Adds the YAML plain scalar"

  override def dependencies = Set.empty
}
