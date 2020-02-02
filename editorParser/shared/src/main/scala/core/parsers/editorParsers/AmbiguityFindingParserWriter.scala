package core.parsers.editorParsers

import core.parsers.core.Metrics

trait AmbiguityFindingParserWriter extends CorrectingParserWriter {

  override def findBestParseResult[Result](parser: BuiltParser[Result], input: Input,
                                           mayStop: StopFunction, metrics: Metrics): SingleParseResult[Result, Input] = {

    val noResultFound = ReadyParseResult(None, input, History.error(FatalError(input, "Grammar is always recursive")))
    var bestResult: ReadyParseResult[Input, Result] = noResultFound

    var resultsSeen = Map.empty[Any, ReadyParseResult[Input, Result]]
    var queue: ParseResults[Input, Result] = parser(input, newParseState(input))
    while(queue.nonEmpty) {
      val (parseResult: LazyParseResult[Input, Result], tail) = queue.pop()

      queue = parseResult match {
        case _parseResult: ReadyParseResult[Input, _] =>
          val parseResult = _parseResult.asInstanceOf[ReadyParseResult[Input, Result]]
          val parseResultKey = ReadyParseResult(parseResult.resultOption, parseResult.remainder, getHistoryWithoutChoices(parseResult.history))
          if (resultsSeen.contains(parseResultKey)) {
            val previousResult = resultsSeen(parseResultKey)
            val oldChoices = getHistoryChoices(previousResult.history)
            val newChoices = getHistoryChoices(parseResult.history)
            val mixedNoDrop = oldChoices.zip(newChoices)

            // TODO equality check hier moet naar reference kijken.
            val mixed = mixedNoDrop.
              dropWhile(t => System.identityHashCode(t._1) == System.identityHashCode(t._2)).
              takeWhile(t => System.identityHashCode(t._1) != System.identityHashCode(t._2)).reverse
            throw new Exception("Your grammar produces duplicates" + previousResult)
          }
          else
            resultsSeen += parseResultKey -> parseResult

          bestResult = if (bestResult.score >= parseResult.score) bestResult else parseResult
          tail match {
            case tailCons: SRCons[Input, _] =>
              if (mayStop(bestResult.remainder.offset, bestResult.originalScore, tailCons.head.score))
                SREmpty.empty
              else
                tail
            case _ =>
              SREmpty.empty
          }
        case delayedResult: DelayedParseResult[Input, _] =>
          val results = delayedResult.results
          tail.merge(results)
      }
    }
    SingleParseResult(bestResult.resultOption, bestResult.history.errors.toList)
  }

  def getHistoryChoices(history: History[Input]): Seq[(Input, Any)] = {
    history match {
      case withChoices: HistoryWithChoices[Input] => withChoices.choices
      case _ => Seq.empty
    }
  }

  def getHistoryWithoutChoices(history: History[Input]): History[Input] = {
    history match {
      case withChoices: HistoryWithChoices[Input] => withChoices.inner
      case _ => history
    }
  }

  override def choice[Result](first: Parser[Result], other: => Parser[Result], firstIsLonger: Boolean = false): Parser[Result] =
    if (firstIsLonger) new TrackingFirstIsLonger(first, other) else new TrackingChoice(first, other)

  class TrackingFirstIsLonger[+First <: Result, +Second <: Result, Result](val first: Parser[First], _second: => Parser[Second])
    extends ParserBuilderBase[Result] with ChoiceLike[Result] {

    lazy val second = _second

    override def getParser(recursive: GetParser): BuiltParser[Result] = {
      val parseFirst = recursive(first)
      lazy val parseSecond = recursive(second)

      (input: Input, state: ParseState) => {
        val firstResult = parseFirst(input, state).addHistory(HistoryWithChoices(Seq(input -> first)))
        val secondResult = parseSecond(input, state).addHistory(HistoryWithChoices(Seq(input -> second)))
        firstResult match {
          case cons: SRCons[Input, Result]
            if !cons.head.history.flawed => firstResult
          case _ =>
            firstResult.merge(secondResult)
        }
      }
    }
  }

  class TrackingChoice[+First <: Result, +Second <: Result, Result](val first: Parser[First], _second: => Parser[Second])
    extends ParserBuilderBase[Result] with ChoiceLike[Result] {

    lazy val second = _second

    override def getParser(recursive: GetParser): BuiltParser[Result] = {
      val parseFirst = recursive(first)
      lazy val parseSecond = recursive(second)

      (input: Input, state: ParseState) => {
        val firstResult = parseFirst(input, state).addHistory(HistoryWithChoices(Seq(input -> first)))
        val secondResult = parseSecond(input, state).addHistory(HistoryWithChoices(Seq(input -> second)))
        val merged = firstResult.merge(secondResult)
        merged
      }
    }
  }
}