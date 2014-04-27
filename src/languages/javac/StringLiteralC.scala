package languages.javac

import transformation.{TransformationState, MetaObject, ProgramTransformation}
import languages.javac.base.JavaBase

object StringLiteralC extends ProgramTransformation {
  object StringClazz
  object ValueField
  def literal(value: String) = {
    new MetaObject(StringClazz) { data.put(ValueField, value) }
  }

  def transform(program: MetaObject, state: TransformationState): Unit = ???

  def dependencies: Set[ProgramTransformation] = Set(JavaBase)
}