package transformations.bytecode.constants

import core.bigrammar.BiGrammar
import core.particles.Language
import core.particles.grammars.GrammarCatalogue
import core.particles.node._
import transformations.bytecode.ByteCodeSkeleton
import transformations.bytecode.PrintByteCode._
import transformations.bytecode.constants.Utf8ConstantDelta.Utf8Constant
import transformations.bytecode.coreInstructions.ConstantPoolIndexGrammar
import transformations.bytecode.extraConstants.TypeConstant.TypeConstantWrapper

object NameAndTypeConstant extends ConstantEntry {

  object Clazz extends NodeClass

  object Name extends NodeField

  object Type extends NodeField

  def nameAndType(nameIndex: Node, typeIndex: Node): Node = new Node(Clazz,
    Name -> nameIndex,
    Type -> typeIndex)

  def nameAndType(nameIndex: Int, typeIndex: Int): Node = new Node(Clazz,
    Name -> nameIndex,
    Type -> typeIndex)

  def getName(nameAndType: Node): Int = nameAndType(Name).asInstanceOf[Int]

  def getTypeIndex(nameAndType: Node): Int = nameAndType(Type).asInstanceOf[Int]

  override def key = Clazz

  implicit class NameAndTypeConstantWrapper[T <: NodeLike](val node: T) extends NodeWrapper[T] {
    def _type: TypeConstantWrapper[T] = node(Type).asInstanceOf[T]
    def _type_=(value: TypeConstantWrapper[T]): Unit = node(Type) = value

    def name: Utf8Constant[T] = node(Name).asInstanceOf[T]
    def name_=(value: Utf8Constant[T]): Unit = node(Name) = value
  }

  override def getByteCode(constant: Node, state: Language): Seq[Byte] = {
    byteToBytes(12) ++ shortToBytes(getName(constant)) ++
      shortToBytes(getTypeIndex(constant))
  }

  override def inject(state: Language): Unit = {
    super.inject(state)
    ByteCodeSkeleton.getState(state).constantReferences.put(key, Map(
      Name -> Utf8ConstantDelta.key,
      Type -> Utf8ConstantDelta.key))
  }

  override def getConstantEntryGrammar(grammars: GrammarCatalogue): BiGrammar =
    ((grammars.find(ConstantPoolIndexGrammar).as(Name) <~ ":") ~
      grammars.find(ConstantPoolIndexGrammar).as(Type)).
    asNode(Clazz)

  override def description: String = "Defines the name and type constant, which contains a name and a field or method descriptor."

  override def getName = "NameAndType"
}