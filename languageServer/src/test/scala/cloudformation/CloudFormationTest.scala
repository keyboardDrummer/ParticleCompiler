package cloudformation

import core.bigrammar.TestLanguageGrammarUtils
import deltas.cloudformation.CloudFormationLanguage
import langserver.types._
import languageServer.lsp._
import languageServer.{HumanPosition, LanguageServerTest, MiksiloLanguageServer}
import org.scalatest.FunSuite
import util.SourceUtils

class CloudFormationTest extends FunSuite with LanguageServerTest {

  val server = new MiksiloLanguageServer(CloudFormationLanguage.language)

  test("No diagnostics") {
    val program = SourceUtils.getTestFileContents("AutoScalingMultiAZWithNotifications.json")
    val result = getDiagnostic(server, program)
    assert(result.size == 1)
  }

  test("Goto definition resource reference") {
    val program = SourceUtils.getTestFileContents("AutoScalingMultiAZWithNotifications.json")
    val result: Seq[Location] = gotoDefinition(server, program, new HumanPosition(365, 37))
    assertResult(Seq(Location(itemUri, Range(new HumanPosition(336,6), new HumanPosition(336,28)))))(result)
  }

  test("Goto definition") {
    val program = SourceUtils.getTestFileContents("AutoScalingMultiAZWithNotifications.json")
    val result: Seq[Location] = gotoDefinition(server, program, new HumanPosition(437, 36))
    assertResult(Seq(Location(itemUri, Range(new HumanPosition(42,6), new HumanPosition(42,17)))))(result)
  }

  test("Goto definition overloaded parameter") {
    val program = SourceUtils.getTestFileContents("AutoScalingMultiAZWithNotifications.json")
    val result = gotoDefinition(server, program, new HumanPosition(445, 32))
    assertResult(Seq(Location(itemUri, Range(new HumanPosition(8,6), new HumanPosition(8,11)))))(result)
  }

  test("Goto definition overloaded parameter second") {
    val program = SourceUtils.getTestFileContents("AutoScalingMultiAZWithNotifications.json")
    val result = gotoDefinition(server, program, new HumanPosition(425, 32))
    assertResult(Seq(Location(itemUri, Range(new HumanPosition(8,6), new HumanPosition(8,11)))))(result)
  }

  test("Code completion parameter") {
    val program = SourceUtils.getTestFileContents("AutoScalingMultiAZWithNotifications.json")
    val result = complete(server, program, new HumanPosition(437, 38))
    val item = CompletionItem("SSHLocation", kind = Some(CompletionItemKind.Text), insertText = Some("cation"))
    assertResult(CompletionList(isIncomplete = false, Seq(item)))(result)
  }

  test("Code completion property") {
    val program = SourceUtils.getTestFileContents("AutoScalingMultiAZWithNotifications.json")
    val result = complete(server, program, new HumanPosition(214, 14))
    val item = CompletionItem("Subscription", kind = Some(CompletionItemKind.Text), insertText = Some("cription"))
    assertResult(CompletionList(isIncomplete = false, Seq(item))) (result)
  }

  test("Code completion overloaded parameter") {
    val program = SourceUtils.getTestFileContents("AutoScalingMultiAZWithNotifications.json")
    val result = complete(server, program, new HumanPosition(425, 32))
    val item = CompletionItem("VpcId", kind = Some(CompletionItemKind.Text), insertText = Some("cId"))
    assertResult(CompletionList(isIncomplete = false, Seq(item)))(result)
  }

  test("Parse example") {
    val utils = new TestLanguageGrammarUtils(CloudFormationLanguage.deltas)
    val source = SourceUtils.getTestFileContents("AutoScalingMultiAZWithNotifications.json")
    utils.parse(source)
  }
}