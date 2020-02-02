package deltas.verilog

import core.SourceUtils
import core.deltas.path.PathRoot
import core.language.node.NodeComparer
import core.language.{Compilation, InMemoryFileSystem}
import core.parsers.editorParsers.SourceRange
import deltas.expression.VariableDelta.Variable
import deltas.expression.{IntLiteralDelta, VariableDelta}
import deltas.statement.{IfThenDelta, IfThenElseDelta}
import deltas.{ClearPhases, FileWithMembersDelta}
import languageServer.{LanguageServerTest, MiksiloLanguageServer}
import lsp.{DocumentPosition, FileRange, HumanPosition, TextDocumentIdentifier}
import org.scalatest.funsuite.AnyFunSuite
import util.TestLanguageBuilder

import scala.reflect.io.Path

class VerilogTest extends AnyFunSuite with LanguageServerTest {

  val language = TestLanguageBuilder.buildWithParser(VerilogLanguage.deltas)
  val justParseLanguage = TestLanguageBuilder.buildWithParser(Seq(ClearPhases) ++ VerilogLanguage.deltas)
  val code = """ module arbiter (
               | clock,
               | reset,
               | req_0,
               | req_1,
               | gnt_0,
               | gnt_1
               | );
               |
               | input clock, reset, req_0, req_1;
               | output gnt_0, gnt_1;
               |
               | reg gnt_0, gnt_1;
               |
               | always @ (posedge clock or posedge reset)
               | if (reset) begin
               |   gnt_0 <= 0;
               |   gnt_1 <= 0;
               | end else if (req_0) begin
               |   gnt_0 <= 1;
               |   gnt_1 <= 0;
               | end else if (req_1) begin
               |   gnt_0 <= 0;
               |   gnt_1 <= 1;
               | end
               |
               | endmodule""".stripMargin

  test("Can parse") {
    val actual = justParseLanguage.compileString(code).program.asInstanceOf[PathRoot].current

    val requestOne = VariableDelta.neww("req_1")
    val requestZero = VariableDelta.neww("req_0")
    val reset = VariableDelta.neww("reset")
    val clock = VariableDelta.neww("clock")
    val grantZero = VariableDelta.neww("gnt_0")
    val grantOne = VariableDelta.neww("gnt_1")

    val thirdIf = IfThenDelta.neww(requestOne, BeginEndDelta.neww(Seq(
      NonBlockingAssignmentDelta.neww("gnt_0", IntLiteralDelta.neww(0)),
      NonBlockingAssignmentDelta.neww("gnt_1", IntLiteralDelta.neww(1)))))
    val secondIf = IfThenElseDelta.neww(requestZero, BeginEndDelta.neww(Seq(
      NonBlockingAssignmentDelta.neww("gnt_0", IntLiteralDelta.neww(1)),
      NonBlockingAssignmentDelta.neww("gnt_1", IntLiteralDelta.neww(0)))),
      thirdIf)
    val firstIf = IfThenElseDelta.neww(reset, BeginEndDelta.neww(Seq(
      NonBlockingAssignmentDelta.neww("gnt_0", IntLiteralDelta.neww(0)),
      NonBlockingAssignmentDelta.neww("gnt_1", IntLiteralDelta.neww(0)))),
      secondIf)
    val moduleMembers = Seq(
      PortTypeSpecifierDelta.neww("input", Seq(clock, reset, requestZero, requestOne)),
      PortTypeSpecifierDelta.neww("output", Seq(grantZero, grantOne)),
      PortTypeSpecifierDelta.neww("reg", Seq(grantZero, grantOne)),
      AlwaysDelta.neww(
        Seq(SensitivityVariableDelta.neww("posedge", clock.name),
          SensitivityVariableDelta.neww("posedge", reset.name)),
        firstIf)
    )
    val ports = Seq(clock, reset, requestZero, requestOne, grantZero, grantOne)
    val module = VerilogModuleDelta.neww("arbiter", ports, moduleMembers)
    val expectation = FileWithMembersDelta.Shape.create(FileWithMembersDelta.Members -> Seq(module))
    expectation.startOfUri = Some(Compilation.singleFileDefaultName)
    assert(NodeComparer().deepEquality(expectation, actual))
  }

  test("Goto definition") {
    val server = new MiksiloLanguageServer(VerilogLanguage.language)
    val first: Seq[FileRange] = gotoDefinition(server, code, new HumanPosition(10, 10))
    assertResult(Seq(FileRange(itemUri, SourceRange(new HumanPosition(2,2), new HumanPosition(2, 7)))))(first)

    val second: Seq[FileRange] = gotoDefinition(server, code, new HumanPosition(15, 21))
    assertResult(Seq(FileRange(itemUri, SourceRange(new HumanPosition(2,2), new HumanPosition(2, 7)))))(second)

    val third: Seq[FileRange] = gotoDefinition(server, code, new HumanPosition(20, 6))
    assertResult(Seq(FileRange(itemUri, SourceRange(new HumanPosition(6,2), new HumanPosition(6, 7)))))(third)
  }

  test("can compile multiple files") {
    val fileSystem = InMemoryFileSystem(Map(
      "./Bus_pkg.sv" -> SourceUtils.getResourceFileContents(Path("verilog") / "Bus_pkg.sv"),
      "testbench.sv" -> SourceUtils.getResourceFileContents(Path("verilog") / "testbench.sv")))
    val compilation = new Compilation(language.language, fileSystem, Some("testbench.sv"))
    compilation.runPhases()
    assert(compilation.diagnostics.isEmpty, compilation.diagnostics)
  }

  test("goto definition works in multiple files") {
    val server = new MiksiloLanguageServer(VerilogLanguage.language)
    val files = Seq(Path("verilog") / "Bus_pkg.sv", Path("verilog") / "testbench.sv")
    for(file <- files) {
      openDocument(server, SourceUtils.getResourceFileContents(file), file.toString())
    }
    val mainIdentifier = TextDocumentIdentifier(files(1).toString())
    val busIdentifier = TextDocumentIdentifier(files(0).toString())

    val gotoPackageResult: Seq[FileRange] = server.gotoDefinition(DocumentPosition(mainIdentifier, new HumanPosition(5, 11)))
    val gotoPackageExpectation = Seq(FileRange(busIdentifier.uri, SourceRange(new HumanPosition(1, 9), new HumanPosition(1, 16))))
    assertResult(gotoPackageExpectation)(gotoPackageResult)

//    val gotoBusResult: Seq[Location] = server.gotoDefinition(DocumentPosition(mainIdentifier, new HumanPosition(7, 5)))
//    val gotoBusExpectation = Seq(Location(busIdentifier.uri, Range(new HumanPosition(8, 9), new HumanPosition(8, 18))))
//    assertResult(gotoBusExpectation)(gotoBusResult)
  }
}