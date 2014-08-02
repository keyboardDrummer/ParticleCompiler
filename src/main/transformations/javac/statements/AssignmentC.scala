package transformations.javac.statements

import core.grammar.{Grammar, seqr}
import core.transformation._
import transformations.bytecode.ByteCode
import transformations.javac.base.JavaMethodC
import transformations.javac.base.model.JavaTypes.{ArrayTypeKey, IntTypeKey, ObjectTypeKey}
import transformations.javac.expressions.ExpressionC

import scala.collection.mutable

object AssignmentC extends GrammarTransformation {
  override def transform(program: MetaObject, state: TransformationState): Unit = {
    ExpressionC.getExpressionToLines(state).put(AssignmentKey, assignment => {
      val methodCompiler = JavaMethodC.getMethodCompiler(state)
      val value = getAssignmentValue(assignment)
      val valueInstructions = ExpressionC.getToInstructions(state)(value)
      val target = getAssignmentTarget(assignment)
      val variable = methodCompiler.variables(target)
      valueInstructions ++ Seq(variable._type.clazz match {
        case IntTypeKey => ByteCode.integerStore(variable.offset)
        case ObjectTypeKey => ByteCode.addressStore(variable.offset)
        case ArrayTypeKey => ByteCode.addressStore(variable.offset)
      })
    })
  }

  def getAssignmentTarget(assignment: MetaObject) = assignment(AssignmentTarget).asInstanceOf[String]

  def getAssignmentValue(assignment: MetaObject) = assignment(AssignmentValue).asInstanceOf[MetaObject]

  /** TODO: separate variableC in a expression and package variable. Make variable, assignment and declaration both dependent on some VariablePoolC.
    * Variable and assignment should further depend on expression.
    */
  override def dependencies: Set[Contract] = Set(JavaMethodC)

  override def transformDelimiters(delimiters: mutable.HashSet[String]): Unit = delimiters += "="

  override def transformGrammars(grammars: GrammarCatalogue): Unit = {
    val expressionGrammar = grammars.find(ExpressionC.ExpressionGrammar)
    val assignmentGrammar: Grammar = (identifier <~ "=") ~ expressionGrammar ^^ { case target seqr value => new MetaObject(AssignmentKey, AssignmentTarget -> target, AssignmentValue -> value)}
    expressionGrammar.inner = expressionGrammar.inner | assignmentGrammar
  }

  object AssignmentKey


  object AssignmentTarget

  object AssignmentValue

}
