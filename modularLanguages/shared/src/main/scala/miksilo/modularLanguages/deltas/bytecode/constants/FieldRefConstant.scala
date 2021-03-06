package miksilo.modularLanguages.deltas.bytecode.constants

import miksilo.modularLanguages.core.bigrammar.BiGrammar
import miksilo.modularLanguages.core.deltas.grammars.LanguageGrammars
import miksilo.modularLanguages.core.node._
import miksilo.languageServer.core.language.{Compilation, Language}
import miksilo.modularLanguages.deltas.bytecode.ByteCodeSkeleton
import miksilo.modularLanguages.deltas.bytecode.PrintByteCode._
import miksilo.modularLanguages.deltas.bytecode.constants.NameAndTypeConstant.NameAndTypeConstantWrapper
import miksilo.modularLanguages.deltas.bytecode.coreInstructions.ConstantPoolIndexGrammar
import miksilo.modularLanguages.deltas.javac.classes.skeleton.QualifiedClassName

object FieldRefConstant extends ConstantPoolEntry {

  object FieldRef extends NodeShape

  object ClassInfo extends NodeField

  object NameAndType extends NodeField

  def fromPrimitives(className: QualifiedClassName, fieldName: String, fieldType: Node) = {
    val classRef = ClassInfoConstant.classRef(className)
    val fieldNameAndType = NameAndTypeConstant.fromNameAndType(fieldName, fieldType)
    FieldRefConstant.fieldRef(classRef, fieldNameAndType)
  }

  implicit class FieldRefWrapper[T <: NodeLike](val node: T) extends NodeWrapper[T] {
    def nameAndType: NameAndTypeConstantWrapper[T] = node(NameAndType).asInstanceOf[T]
    def nameAndType_=(value: NameAndTypeConstantWrapper[T]): Unit = node(NameAndType) = value

    def nameAndTypeIndex: Int = node(NameAndType).asInstanceOf[Int]
    def nameAndTypeIndex_=(value: Int): Unit = node(NameAndType) = value

    def classIndex: Int = node(ClassInfo).asInstanceOf[Int]
    def classIndex_=(value: Int): Unit = node(ClassInfo) = value
  }

  def fieldRef(classConstant: Node, nameAndType: Node) = new Node(FieldRef,
    ClassInfo -> classConstant,
    NameAndType -> nameAndType)

  def fieldRef(classIndex: Int, nameAndTypeIndex: Int) = new Node(FieldRef,
    ClassInfo -> classIndex,
    NameAndType -> nameAndTypeIndex)

  override def getBytes(compilation: Compilation, constant: Node): Seq[Byte] = {
    val fieldRef: FieldRefWrapper[Node] = constant
    byteToBytes(9) ++
      shortToBytes(fieldRef.classIndex) ++
      shortToBytes(fieldRef.nameAndTypeIndex)
  }

  override def inject(language: Language): Unit = {
    super.inject(language)
    ByteCodeSkeleton.constantReferences.add(language, shape, Map(
      ClassInfo -> ClassInfoConstant.shape,
      NameAndType -> NameAndTypeConstant.shape))
  }

  override def shape = FieldRef

  override def getConstantEntryGrammar(grammars: LanguageGrammars): BiGrammar = {
    import grammars._
    find(ConstantPoolIndexGrammar).as(ClassInfo) ~< "." ~
      find(ConstantPoolIndexGrammar).as(NameAndType)
  }

  override def description: String = "Defines the field reference constant, which reference to a field by class name, field name and type."

  override val getName = "Fieldref"
}
