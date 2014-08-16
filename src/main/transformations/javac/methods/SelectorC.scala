package transformations.javac.methods


import core.transformation._
import core.transformation.grammars.GrammarCatalogue
import transformations.bytecode.coreInstructions.GetStaticC
import transformations.javac.classes._
import transformations.javac.expressions.{ExpressionC, ExpressionInstance}

object SelectorC extends ExpressionInstance {

  override val key: AnyRef = SelectorKey

  override def dependencies: Set[Contract] = Set(MethodC, GetStaticC)

  def selector(_object: Any, member: Any): MetaObject = selector(_object.asInstanceOf[MetaObject], member.asInstanceOf[String])

  def selector(_object: MetaObject, member: String): MetaObject = {
    new MetaObject(SelectorKey) {
      data.put(SelectorObject, _object)
      data.put(SelectorMember, member)
    }
  }

  override def getType(selector: MetaObject, state: TransformationState): MetaObject = {
    val compiler = ClassC.getClassCompiler(state)
    val obj = getSelectorObject(selector)
    val member = getSelectorMember(selector)
    val classOrObjectReference = compiler.getReferenceKind(obj).asInstanceOf[ClassOrObjectReference]
    val fieldInfo = classOrObjectReference.info.getField(member)
    fieldInfo._type
  }

  def getSelectorObject(selector: MetaObject) = selector(SelectorObject).asInstanceOf[MetaObject]

  def getSelectorMember(selector: MetaObject) = selector(SelectorMember).asInstanceOf[String]

  override def toByteCode(selector: MetaObject, state: TransformationState): Seq[MetaObject] = {
    val compiler = ClassC.getClassCompiler(state)
    val obj = getSelectorObject(selector)
    val classOrObjectReference = compiler.getReferenceKind(obj).asInstanceOf[ClassOrObjectReference]
    val member = getSelectorMember(selector)
    val fieldInfo = classOrObjectReference.info.getField(member)
    val fieldRef = compiler.getFieldRefIndex(fieldInfo)
    if (classOrObjectReference.wasClass)
      Seq(GetStaticC.getStatic(fieldRef))
    else
      ???
  }

  override def transformGrammars(grammars: GrammarCatalogue): Unit = {
    val core = grammars.find(ExpressionC.CoreGrammar)
    val expression = grammars.find(ExpressionC.ExpressionGrammar)
    val selection = (expression <~ ".") ~ identifier ^^ { case left seqr right => selector(left, right)}
    core.inner = core.inner | selection
  }

  object SelectorKey

  object SelectorObject

  object SelectorMember

}
