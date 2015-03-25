package core.particles

import core.particles.node.Node
import org.junit.{Assert, Test}
import transformations.javac.expressions.literals.IntLiteralC
import transformations.javac.methods.call.CallC
import transformations.javac.methods.{MemberSelector, VariableC}

class TestMetaObject {

  @Test
  def testEquals() {
    val first = new Node(ClazzKey, FieldKey -> FieldValue)
    val second = new Node(ClazzKey, FieldKey -> FieldValue)
    Assert.assertEquals(first, second)
  }

  @Test
  def testEqualsOnJavaModel() {
    val first = CallC.call(MemberSelector.selector(MemberSelector.selector(VariableC.variable("System"), "out"), "print"),
      List(CallC.call(VariableC.variable("fibonacci"), List(IntLiteralC.literal(5)))))
    val second = CallC.call(MemberSelector.selector(MemberSelector.selector(VariableC.variable("System"), "out"), "print"),
      List(CallC.call(VariableC.variable("fibonacci"), List(IntLiteralC.literal(5)))))
    Assert.assertEquals(first, second)
  }

  object ClazzKey

  object FieldKey

  object FieldValue


}
