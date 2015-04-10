package transformations.bytecode.readJar

import java.io
import java.io.InputStream

import application.compilerCockpit.PrettyPrint
import core.bigrammar.TestGrammarUtils
import core.particles.CompilerFromParticles
import org.junit.{Ignore, Assert, Test}
import transformations.bytecode.types.TypeSkeleton.ByteCodeTypeGrammar
import transformations.javac.JavaCompiler
import util.TestUtils

import scala.reflect.io.{File, Path}

class TestClassFileDecompiler {

  @Ignore
  @Test
  def decompileRuntimeJar() = {
    val currentDir = new File(new io.File("."))
    val testResources = currentDir / Path("resources") / "rtUnzipped"
    val allCassFiles = testResources.toDirectory.deepFiles
    val compiler: CompilerFromParticles = new CompilerFromParticles(Seq(new PrettyPrint()) ++ ClassFileSignatureDecompiler.byteCodeParticles)
    var counter = 0
    for(file <- allCassFiles) {
      val inputStream = file.inputStream()
      Console.println(s"starting: ${file.name}")
      compiler.parseAndTransform(inputStream)
      counter += 1
      Console.println(s"progress: $counter / 19.626")
    }
  }

  @Test
  def testObjectClassUnParsedAttributes() = {
    val inputStream = getInputStream("Object.class")
    val compiler: CompilerFromParticles = new CompilerFromParticles(Seq(new PrettyPrint()) ++ ClassFileSignatureDecompiler.byteCodeParticles)
    val state = compiler.parseAndTransform(inputStream)

    val expected = TestUtils.getTestFile("DecodedObjectClassPrettyPrint.txt").slurp()
    Assert.assertEquals(expected, state.output)
  }

  @Test
  def testObjectClassParsedAttributes() = {
    val compiler = new CompilerFromParticles(Seq(ParseKnownAttributes) ++ Seq(new PrettyPrint()) ++
      ClassFileSignatureDecompiler.onlySignatureAttribute)
    val state = compiler.parseAndTransform(getInputStream("Object.class"))

    val expected = TestUtils.getTestFile("DecodedWithAttributesObjectClassPrettyPrint.txt").slurp()
    Assert.assertEquals(expected, state.output)
  }

  @Test
  def testObjectClassSignatureDeCompilation() = {
    val compiler = new CompilerFromParticles(ClassFileSignatureDecompiler.getDecompiler)
    val state = compiler.parseAndTransform(getInputStream("Object.class"))
    val outputState = new CompilerFromParticles(Seq(new PrettyPrint()) ++ JavaCompiler.javaCompilerTransformations).transformReturnState(state.program)

    val expected = TestUtils.getTestFile("DecompiledObjectClassFileSignature.txt").slurp()
    Assert.assertEquals(expected, outputState.output)
  }

  @Test
  def testStringClassSignatureDeCompilation() = {
    val compiler = new CompilerFromParticles(ClassFileSignatureDecompiler.getDecompiler)
    val state = compiler.parseAndTransform(getInputStream("String.class"))
    val outputState = new CompilerFromParticles(Seq(new PrettyPrint()) ++ JavaCompiler.javaCompilerTransformations).transformReturnState(state.program)

    val expected = TestUtils.getTestFile("DecompiledStringClassFileSignature.txt").slurp()
    Assert.assertEquals(expected, outputState.output)
  }

  @Test
  def testSystemClassSignatureDeCompilation() = {
    val compiler = new CompilerFromParticles(ClassFileSignatureDecompiler.getDecompiler)
    val state = compiler.parseAndTransform(getInputStream("System.class"))
    val outputState = new CompilerFromParticles(Seq(new PrettyPrint()) ++ JavaCompiler.javaCompilerTransformations).transformReturnState(state.program)

    val expected = TestUtils.getTestFile("DecompiledSystemClassFileSignature.txt").slurp()
    Assert.assertEquals(expected, outputState.output)
  }

  @Test
  def testPrintStreamClassSignatureDeCompilation() = {
    val compiler = new CompilerFromParticles(ClassFileSignatureDecompiler.getDecompiler)
    val state = compiler.parseAndTransform(getInputStream("PrintStream.class"))
    val outputState = new CompilerFromParticles(Seq(new PrettyPrint()) ++ JavaCompiler.javaCompilerTransformations).transformReturnState(state.program)

    val expected = TestUtils.getTestFile("DecompiledPrintStreamClassFileSignature.txt").slurp()
    Assert.assertEquals(expected, outputState.output)
  }

  def getInputStream(fileName: Path): InputStream = {
    val file = TestUtils.getTestFile(fileName)
    file.inputStream()
  }

  @Test
  def testParseByteCodeType3() = {
    val grammarUtils = TestGrammarUtils(ClassFileSignatureDecompiler.getDecompiler)
    grammarUtils.compareInputWithPrint("([BII)V", None, ByteCodeTypeGrammar)
  }

  @Test
  def testParseByteCodeType2() = {
    val grammarUtils = TestGrammarUtils(ClassFileSignatureDecompiler.getDecompiler)
    grammarUtils.compareInputWithPrint("[B", None, ByteCodeTypeGrammar)
  }

  @Test
  def testParseByteCodeType1() = {
    val grammarUtils = TestGrammarUtils(ClassFileSignatureDecompiler.getDecompiler)
    grammarUtils.compareInputWithPrint("B", None, ByteCodeTypeGrammar)
  }

  @Test
  def testParseByteCodeType0() = {
    val grammarUtils = TestGrammarUtils(ClassFileSignatureDecompiler.getDecompiler)
    grammarUtils.compareInputWithPrint("I", None, ByteCodeTypeGrammar)
  }
}