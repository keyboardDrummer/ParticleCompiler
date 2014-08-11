package transformations.javac

import core.transformation.sillyCodePieces.ProgramTransformation
import core.transformation.{Contract, MetaObject, TransformationState}
import transformations.javac.base.MethodAndClassC
import transformations.javac.expressions.ExpressionC
import transformations.javac.methods.{CallC, SelectorC, VariableC}

object ImplicitThisInPrivateCalls extends ProgramTransformation {
  val thisName: String = "this"

  override def dependencies: Set[Contract] = Set(CallC, VariableC)

  override def transform(program: MetaObject, state: TransformationState): Unit = {
    val original = ExpressionC.getExpressionToLines(state)(CallC.CallKey)
    ExpressionC.getExpressionToLines(state).put(CallC.CallKey, (call: MetaObject) => {
      val callCallee = CallC.getCallCallee(call)
      val compiler = MethodAndClassC.getMethodCompiler(state)
      if (callCallee.clazz == VariableC.VariableKey) {
        val memberName = VariableC.getVariableName(callCallee)
        val currentClass = compiler.classCompiler.currentClassInfo
        val methodInfo = currentClass.getMethod(memberName)
        val selectee = VariableC.variable(if (methodInfo._static) currentClass.name else thisName)
        call(CallC.CallCallee) = SelectorC.selector(selectee, memberName)
      }
      original(call)
    })
  }
}
