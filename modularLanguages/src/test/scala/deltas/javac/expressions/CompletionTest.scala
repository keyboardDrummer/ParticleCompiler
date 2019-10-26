package deltas.javac.expressions

import deltas.javac.JavaLanguage
import languageServer.{LanguageServerTest, MiksiloLanguageServer}
import lsp.{CompletionItem, CompletionItemKind}
import lsp.CompletionList
import lsp.HumanPosition
import org.scalatest.FunSuite
import util.JavaSourceUtils

class CompletionTest extends FunSuite with LanguageServerTest {

  val server = new MiksiloLanguageServer(JavaLanguage.java)

  test("fibonacci") {
    val program = JavaSourceUtils.getJavaTestFileContents("Fibonacci")
    val indexDefinition = complete(server, program, new HumanPosition(5, 40))
    val item = createCompletionItem("fibonacci")
    assertResult(CompletionList(isIncomplete = false, Seq(item)))(indexDefinition)
  }
}