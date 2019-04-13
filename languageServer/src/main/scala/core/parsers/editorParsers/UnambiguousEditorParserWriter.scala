package core.parsers.editorParsers

import core.parsers.core.UnambiguousParserWriter

trait UnambiguousEditorParserWriter extends UnambiguousParserWriter with EditorParserWriter {

  override def abort = RecursionDetectedOrNoFailure

  object RecursionDetectedOrNoFailure extends OptionFailure[Nothing] with UnamEditorParseResult[Nothing] {
    override def offset: Int = -1

    override def map[NewResult](f: Nothing => NewResult): OptionFailure[NewResult] with ParseResult[NewResult] = this

    override def partialResult: Option[Nothing] = None

    override def updateRemainder(f: Input => Input) = this

    override def flatMap[NewResult](f: Success[Nothing] => UnamEditorParseResult[NewResult]) = this

    override def successful = false

    override def resultOption = None

    override def successOption = None

    override def biggestFailure = this

    override def getSuccessRemainder = None

    override def addFailure[Other >: Nothing](other: OptionFailure[Other]) = this

    override def addDefault[Other >: Nothing](value: Other, force: Boolean) = this
  }

  trait UnamEditorParseResult[+Result] extends UnambiguousParseResult[Result] with EditorResult[Result] {
    def successOption: Option[Success[Result]]
    def addFailure[Other >: Result](other: OptionFailure[Other]): ParseResult[Other]

    def biggestRealFailure: Option[ParseFailure[Result]] = biggestFailure match {
      case failure: ParseFailure[Result] => Some(failure)
      case _ => None
    }
  }

  type ParseResult[+R] = UnamEditorParseResult[R]

  override def newFailure[Result](partial: Option[Result], input: Input, message: String) = EditorParseResult(None, ParseFailure(partial, input, message))

  override def newSuccess[Result](result: Result, remainder: Input) = EditorParseResult(Some(Success(result, remainder)), RecursionDetectedOrNoFailure)

  override def newFailure[Result](input: Input, message: String) = EditorParseResult(None, ParseFailure(None, input, message))

  override def leftRight[Left, Right, NewResult](left: EditorParser[Left],
                                                 right: => EditorParser[Right],
                                                 combine: (Left, Right) => NewResult): EditorParser[NewResult] =
    new Sequence(left, right, combine)

  override def succeed[NR](result: NR): EditorParser[NR] = Succeed(result)

  override def choice[Result](first: EditorParser[Result], other: => EditorParser[Result], leftIsAlwaysBigger: Boolean): EditorParser[Result] =
    if (leftIsAlwaysBigger) new OrElse(first, other) else new BiggestOfTwo(first, other)

  override def map[Result, NewResult](original: Self[Result], f: Result => NewResult): Self[NewResult] = new MapParser(original, f)

  override def lazyParser[Result](inner: => EditorParser[Result]) = new EditorLazy(inner)

  override def parseWholeInput[Result](parser: EditorParser[Result], input: Input) = {

    val parseResult = parser.parseRoot(input)
    parseResult.successOption match {
      case Some(success) =>
        if (success.remainder.atEnd) parseResult
        else {
          val failedSuccess = ParseFailure(Some(success.result), success.remainder, "Did not parse entire input")
          EditorParseResult(None, failedSuccess.getBiggest(parseResult.biggestFailure))
        }
      case None => parseResult
    }
  }

  override def newParseState(parser: EditorParser[_]) = new LeftRecursionDetectorState()

  class Sequence[+Left, +Right, Result](val left: EditorParser[Left],
                                         _right: => EditorParser[Right],
                                         combine: (Left, Right) => Result)
    extends EditorParserBase[Result] with SequenceLike[Result] {

    lazy val right: EditorParser[Right] = _right

    override def getParser(recursive: GetParse): Parse[Result] = {
      val parseLeft = recursive(left)
      lazy val parseRight = recursive(right)

      def apply(input: Input) = {
        val leftResult = parseLeft(input)
        val leftFailure = right.default.map(rightDefault => leftResult.biggestFailure.map(l => combine(l, rightDefault))).
          getOrElse(leftResult.biggestFailure match {
            case failure: ParseFailure[Left] => ParseFailure(None, failure.remainder, failure.message)
            case RecursionDetectedOrNoFailure => RecursionDetectedOrNoFailure
          })
        leftResult.successOption match {
          case Some(leftSuccess) =>
            val rightResult = parseRight(leftSuccess.remainder)
            rightResult.map(r => combine(leftSuccess.result, r)).addFailure(leftFailure)

          case None =>
            EditorParseResult(None, leftFailure)
        }
      }

      apply
    }


    override def getDefault(cache: DefaultCache): Option[Result] = for {
      leftDefault <- cache(left)
      rightDefault <- cache(right)
    } yield combine(leftDefault, rightDefault)
  }

  class OrElse[+First <: Result, +Second <: Result, Result](val first: EditorParser[First], _second: => EditorParser[Second])
    extends EditorParserBase[Result] with ChoiceLike[Result] {

    lazy val second = _second


    override def getParser(recursive: GetParse): Parse[Result] = {
      val parseFirst = recursive(first)
      val parseSecond = recursive(second)

      def apply(input: Input) = {
        val firstResult = parseFirst(input)
        val result = firstResult.successOption match {
          case Some(_) => firstResult
          case None =>
            val secondResult = parseSecond(input)
            secondResult.addFailure(firstResult.biggestFailure)
        }
        default.fold[ParseResult[Result]](result)(d => result.addDefault[Result](d, force = false))
      }

      apply
    }


    override def getDefault(cache: DefaultCache): Option[Result] = {
      val value: Option[First] = cache(first)
      value.orElse(cache(second))
    }
  }

  class BiggestOfTwo[+First <: Result, +Second <: Result, Result](val first: EditorParser[First], _second: => EditorParser[Second])
    extends EditorParserBase[Result] with ChoiceLike[Result] {

    lazy val second = _second


    override def getParser(recursive: GetParse): Parse[Result] = {
      val parseFirst = recursive(first)
      lazy val parseSecond = recursive(second)

      def apply(input: Input) = {
        val firstResult = parseFirst(input)
        val secondResult = parseSecond(input)
        val result = (firstResult.successOption, secondResult.successOption) match {
          case (Some(firstSuccess), Some(secondSuccess)) =>
            if (firstSuccess.remainder.offset >= secondSuccess.remainder.offset)
              firstResult.addFailure(secondResult.biggestFailure)
            else
              secondResult.addFailure(firstResult.biggestFailure)
          case (None, Some(_)) =>
            secondResult.addFailure(firstResult.biggestFailure)
          case (Some(_), None) =>
            firstResult.addFailure(secondResult.biggestFailure)
          case (None, None) =>
            firstResult.addFailure(secondResult.biggestFailure)
          case _ => throw new Exception("can not occur")
        }
        default.fold[ParseResult[Result]](result)(d => result.addDefault[Result](d))
      }

      apply
    }

    override def getDefault(cache: DefaultCache): Option[Result] = {
      val value: Option[First] = cache(first)
      value.orElse(cache(second))
    }
  }

  final case class EditorParseResult[+Result](successOption: Option[Success[Result]], biggestFailure: OptionFailure[Result])
    extends UnambiguousParseResult[Result] with EditorResult [Result] with UnamEditorParseResult[Result] {

    def resultOption: Option[Result] = successOption.map(s => s.result).orElse(biggestFailure.partialResult)
    def remainder: Input = getSuccessRemainder.getOrElse(biggestFailure.asInstanceOf[ParseFailure[Result]].remainder)

    def addDefault[Other >: Result](value: Other, force: Boolean): EditorParseResult[Other] = biggestFailure match {
      case f: ParseFailure[Result] => EditorParseResult(successOption, f.addDefault(value, force))
      case RecursionDetectedOrNoFailure => this
    }

    def updateRemainder(f: Input => Input): EditorParseResult[Result] = {
      EditorParseResult(successOption.map(s => Success(s.result, f(s.remainder))), biggestFailure.updateRemainder(f))
    }

    override def getSuccessRemainder = successOption.map(s => s.remainder)

    override def map[NewResult](f: Result => NewResult): ParseResult[NewResult] = {

      val failure = biggestFailure match {
        case failure: ParseFailure[Result] =>
          ParseFailure(failure.partialResult.map(r => f(r)), failure.remainder, failure.message)
        case RecursionDetectedOrNoFailure => RecursionDetectedOrNoFailure
      }
      EditorParseResult(successOption.map(s => s.map(f)), failure)
    }

    override def flatMap[NewResult](f: Success[Result] => ParseResult[NewResult]): ParseResult[NewResult] = {

      val failure = biggestFailure match {
        case failure: ParseFailure[Result] =>
          val newResult = failure.partialResult.flatMap(r => f(Success(r, failure.remainder)).resultOption)
          ParseFailure(newResult, failure.remainder, failure.message)
        case RecursionDetectedOrNoFailure => RecursionDetectedOrNoFailure
      }
      successOption.map(s => f(s).addFailure(failure)).getOrElse(EditorParseResult(None, failure))
    }

    def addFailure[Other >: Result](other: OptionFailure[Other]): EditorParseResult[Other] =
      if (biggestFailure.offset >= other.offset || successOption.exists(s => s.remainder.offset >= other.offset)) this else
        EditorParseResult(successOption, other)
  }

}