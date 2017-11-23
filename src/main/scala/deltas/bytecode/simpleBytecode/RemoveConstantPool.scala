package deltas.bytecode.simpleBytecode

import core.bigrammar.{GrammarReference, RootGrammar}
import core.deltas.grammars.LanguageGrammars
import core.deltas.node._
import core.deltas.path.PathRoot
import core.deltas.{Compilation, DeltaWithGrammar, DeltaWithPhase, Language}
import deltas.bytecode.ByteCodeSkeleton
import deltas.bytecode.ByteCodeSkeleton.ConstantPoolGrammar
import deltas.bytecode.constants.FieldRefConstant.ClassInfo
import deltas.bytecode.constants.MethodRefConstant.{ClassRef, MethodRefKey}
import deltas.bytecode.constants.NameAndTypeConstant.Type
import deltas.bytecode.constants._
import deltas.bytecode.coreInstructions.ConstantPoolIndexGrammar
import deltas.bytecode.extraConstants.{QualifiedClassNameConstantDelta, TypeConstant}
import deltas.javac.classes.ConstantPool

object RemoveConstantPool extends DeltaWithPhase with DeltaWithGrammar {
  override def transform(program: Node, state: Compilation): Unit = {
    val pool = new ConstantPool()
    program(ByteCodeSkeleton.ClassConstantPool) = pool
    val constantReferences = ByteCodeSkeleton.getRegistry(state).constantReferences

    PathRoot(program).visit(afterChildren = node => constantReferences.get(node.clazz).foreach(reference => {
      for (entry <- reference) {
        node.current.get(entry._1).foreach(fieldValue => {
          val index = pool.store(fieldValue)
          node.current.data.put(entry._1, index)
        })
      }
    }))
  }

  override def transformGrammars(_grammars: LanguageGrammars, language: Language): Unit = {
    val grammars = _grammars
    import _grammars._

    val constantReferences = ByteCodeSkeleton.getRegistry(language).constantReferences

    val constantPoolIndexGrammar = grammars.find(ConstantPoolIndexGrammar)
    for(containerEntry <- constantReferences) {
      val key: GrammarKey = containerEntry._1
      val constantFields: Map[NodeField, NodeClass] = containerEntry._2
      val keyGrammar = new RootGrammar(grammars.find(key))
      for(field <- constantFields) {
        val asGrammar = keyGrammar.findAs(field._1)
        val constantRef = asGrammar.findGrammar(constantPoolIndexGrammar).get.asInstanceOf[GrammarReference]
        constantRef.set(grammars.find(field._2))
      }
    }

    grammars.find(Utf8ConstantDelta.key).inner = Utf8ConstantDelta.getConstantEntryGrammar(grammars) asNode Utf8ConstantDelta.key
    grammars.find(TypeConstant.key).inner = TypeConstant.getConstantEntryGrammar(grammars) asNode TypeConstant.key
    grammars.find(QualifiedClassNameConstantDelta.key).inner = QualifiedClassNameConstantDelta.getConstantEntryGrammar(grammars) asNode QualifiedClassNameConstantDelta.key
    grammars.find(MethodRefConstant.key).inner = (grammars.find(ClassInfoConstant.key).as(ClassRef) ~< "." ~
      grammars.find(NameAndTypeConstant.key).as(MethodRefConstant.NameAndType)) asNode MethodRefKey
    grammars.find(ClassInfoConstant.key).inner = grammars.find(QualifiedClassNameConstantDelta.key).as(ClassInfoConstant.Name) asNode ClassInfoConstant.Clazz
    grammars.find(FieldRefConstant.key).inner = grammars.find(ClassInfoConstant.key).as(ClassInfo) ~ "." ~
      grammars.find(NameAndTypeConstant.key).as(FieldRefConstant.NameAndType) asNode FieldRefConstant.key
    grammars.find(NameAndTypeConstant.key).inner = grammars.find(Utf8ConstantDelta.key).as(NameAndTypeConstant.Name) ~~
      grammars.find(TypeConstant.key).as(Type) asNode NameAndTypeConstant.Clazz

    val constantPoolGrammar = language.grammars.root.findLabelled(ConstantPoolGrammar)
    constantPoolGrammar.previous.asInstanceOf[GrammarReference].removeMeFromSequence()
  }

  override def description: String = "Removes the constant pool in favor of inline constant entries"
}