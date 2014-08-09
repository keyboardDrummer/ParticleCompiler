package transformations.bytecode.extraBytecodeInstructions

import core.transformation.{CompilerFromTransformations, MetaObject}
import org.junit.{Assert, Test}
import transformations.bytecode.ByteCodeSkeleton
import transformations.bytecode.extraBooleanInstructions.OptimizeBooleanInstructionsC
import transformations.javac.{JavaCompiler, TestUtils}

import scala.reflect.io.Path

class TestOptimizeBooleanInstructions {

  @Test
  def testForFibonacci() {
    val withOptimization = TestUtils.parseAndTransform("fibonacci", Path("fibonacciWithMain"), JavaCompiler.getCompiler)
    val withoutOptimizationTransformations = JavaCompiler.javaCompilerTransformations.filter(i => i != OptimizeBooleanInstructionsC)
    val withoutOptimization = TestUtils.parseAndTransform("fibonacci", Path("fibonacciWithMain"), new CompilerFromTransformations(withoutOptimizationTransformations))

    val unoptimizedInstructions = getFibonacciInstructions(withoutOptimization)
    val optimizedInstructions = getFibonacciInstructions(withOptimization)

    Assert.assertTrue(s"optimizedInstructions.size (${optimizedInstructions.size}) + 5 < unoptimizedInstructions.size (${unoptimizedInstructions.size})",
      optimizedInstructions.size + 3 < unoptimizedInstructions.size)
  }

  def getFibonacciInstructions(clazz: MetaObject) = {
    ByteCodeSkeleton.getMethods(clazz)
      .flatMap(methodInfo => ByteCodeSkeleton.getMethodAttributes(methodInfo))
      .flatMap(annotation => if (annotation.clazz == ByteCodeSkeleton.CodeKey) Some(annotation) else None)
      .flatMap(codeAnnotation => ByteCodeSkeleton.getCodeInstructions(codeAnnotation))
  }
}
