package miksilo.editorParser.parsers

import miksilo.editorParser.languages.json.JsonParser
import miksilo.editorParser.parsers.editorParsers.{LeftRecursiveCorrectingParserWriter, ParseError, Position, SourceRange, UntilBestAndXStepsStopFunction}
import miksilo.editorParser.parsers.strings.{CommonParserWriter, NoStateParserWriter, WhitespaceParserWriter}
import org.scalatest.funsuite.AnyFunSuite

class WhiteSpaceTest extends AnyFunSuite {

  val parser = ExpressionParser.root.getWholeInputParser()

  test("empty JSON object") {
    val program = "{ }"

    val result = JsonParser.parser.parse(program)
    assert(result.errors.isEmpty)
  }

  test("empty nested JSON object") {
    val program = """{ "bla": { }}"""

    val result = JsonParser.parser.parse(program)
    assert(result.errors.isEmpty)
  }

  test("diagnostics placement in whitespace 1") {
    val program = "    "

    val result = parser.parse(program, UntilBestAndXStepsStopFunction())
    assertError(result.errors.head, SourceRange(Position(0,0),Position(0,4)), "expected '<expression>'")
  }

  test("diagnostics placement in whitespace 2") {
    val program = "   + 3"

    val result = parser.parse(program)
    assertError(result.errors.head, SourceRange(Position(0,0),Position(0,3)), "expected '<expression>'")
  }

  test("diagnostics placement in whitespace 3") {
    val program = "3 +    "

    val result = parser.parse(program, UntilBestAndXStepsStopFunction())
    assertError(result.errors.head, SourceRange(Position(0,3),Position(0,7)), "expected '<expression>'")
  }

  test("diagnostics placement in whitespace 5") {
    val program = "let   = 3 in abc"

    val result = parser.parse(program, UntilBestAndXStepsStopFunction())
    assertError(result.errors.head, SourceRange(Position(0,3),Position(0,4)), "expected '<expression>'")
  }

  test("diagnostics placement in whitespace 6") {
    val program = "let abc =      in abc"

    val result = parser.parse(program, UntilBestAndXStepsStopFunction())
    assertError(result.errors.head, SourceRange(Position(0,9),Position(0,15)), "expected '<expression>'")
  }

  def assertError(error: ParseError, range: SourceRange, message: String): Unit = {
    assertResult(range)(error.range)
    assertResult("expected '<expression>'")(message)
  }
}

object ExpressionParser extends CommonParserWriter with NoStateParserWriter
  with LeftRecursiveCorrectingParserWriter with WhitespaceParserWriter {

  lazy val expression: Parser[Any] = new Lazy(addition | numberParser | let | variable | hole)

  val numberParser: Parser[Any] = wholeNumber

  val addition = expression ~< "+" ~ expression

  val variable = parseIdentifier.filter(s => s != "in", x => "")
  val variableDeclaration = parseIdentifier
  val let = "let" ~> variableDeclaration ~< "=" ~ expression ~< "in" ~ expression
  val hole = Fallback(RegexParser(" *".r, "spaces"), "expression")

  val root = expression ~< trivias
}
