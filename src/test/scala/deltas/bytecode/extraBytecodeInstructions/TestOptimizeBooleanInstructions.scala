package deltas.bytecode.extraBytecodeInstructions

import core.deltas.node.Node
import org.scalatest.FunSuite
import deltas.bytecode.ByteCodeSkeleton.ByteCodeWrapper
import deltas.bytecode.extraBooleanInstructions.OptimizeComparisonInstructionsC
import deltas.javac.JavaCompilerDeltas
import util.CompilerBuilder
import util.TestUtils

import scala.reflect.io.Path

class TestOptimizeBooleanInstructions extends FunSuite {

  test("ForFibonacci") {
    val withOptimization = TestUtils.parseAndTransform("Fibonacci", Path(""))
    val withoutOptimizationTransformations = JavaCompilerDeltas.javaCompilerDeltas.filter(i => i != OptimizeComparisonInstructionsC)
    val withoutOptimization = new TestUtils(CompilerBuilder.build(withoutOptimizationTransformations)).parseAndTransform("Fibonacci", Path(""))

    val unoptimizedInstructions = getFibonacciInstructions(withoutOptimization)
    val optimizedInstructions = getFibonacciInstructions(withOptimization)

    assert(optimizedInstructions.size + 3 < unoptimizedInstructions.size,
      s"optimizedInstructions.size (${optimizedInstructions.size}) + 5 < unoptimizedInstructions.size (${unoptimizedInstructions.size})")
  }

  def getFibonacciInstructions(clazz: ByteCodeWrapper[Node]) = {
    clazz.methods.flatMap(methodInfo => methodInfo.codeAttribute.instructions)
  }
}
