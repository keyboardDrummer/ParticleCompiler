package transformations.javac.expressions

import core.transformation._
import transformations.bytecode.ByteCode
import transformations.javac.base.JavaBase

object LiteralC extends GrammarTransformation {
  def literal(value: AnyVal) = {
    new MetaObject(LiteralKey) {
      data.put(ValueKey, value)
    }
  }

  def getValue(literal: MetaObject) = {
    literal(ValueKey)
  }

  object LiteralKey

  object ValueKey

  override def transform(program: MetaObject, state: TransformationState): Unit = {
    JavaBase.getStatementToLines(state).put(LiteralKey, (literal: MetaObject, compiler) => {
      val value = getValue(literal)
      Seq(value match {
        case i: Integer => ByteCode.integerConstant(i)
        case b: Boolean => ByteCode.integerConstant(if (b) 1 else 0)
      })
    })
  }

  override def dependencies: Set[ProgramTransformation] = Set(JavaBase)


  override def transformGrammars(grammars: GrammarCatalogue) = {
    val parseNumber = number ^^ (number => LiteralC.literal(Integer.parseInt(number.asInstanceOf[String])))
    val expressionGrammar = grammars.find(AddExpression.ExpressionGrammar)
    expressionGrammar.inner = expressionGrammar.inner | parseNumber
    grammars
  }
}
