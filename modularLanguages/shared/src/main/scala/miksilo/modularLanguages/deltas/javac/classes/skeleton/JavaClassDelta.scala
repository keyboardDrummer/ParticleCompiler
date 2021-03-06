package miksilo.modularLanguages.deltas.javac.classes.skeleton

import miksilo.modularLanguages.core.bigrammar.BiGrammar
import miksilo.modularLanguages.core.bigrammar.grammars.BiChoice
import miksilo.modularLanguages.core.deltas._
import miksilo.modularLanguages.core.deltas.grammars.{BodyGrammar, LanguageGrammars}
import miksilo.modularLanguages.core.deltas.path.{NodeChildPath, NodePath}
import miksilo.editorParser.document.BlankLine
import miksilo.modularLanguages.core.node._
import miksilo.languageServer.core.language.{Compilation, CompilationField, Language}
import miksilo.languageServer.core.smarts.ConstraintBuilder
import miksilo.languageServer.core.smarts.objects.{Declaration, NamedDeclaration}
import miksilo.languageServer.core.smarts.scopes.objects.{Scope, ScopeVariable}
import miksilo.languageServer.core.smarts.types.DeclarationHasType
import miksilo.languageServer.core.smarts.types.objects.TypeFromDeclaration
import miksilo.modularLanguages.deltas.ConstraintSkeleton
import miksilo.modularLanguages.deltas.bytecode.types.{ArrayTypeDelta, QualifiedObjectTypeDelta, TypeSkeleton, UnqualifiedObjectTypeDelta}
import miksilo.modularLanguages.deltas.classes.{ClassDelta, HasConstraintsDelta}
import miksilo.modularLanguages.deltas.classes.ClassDelta.{ClassImports, ClassPackage, ClassParent, JavaClass, Members, Shape}
import miksilo.modularLanguages.deltas.javac.classes.{ClassCompiler, FieldDeclarationDelta}
import miksilo.modularLanguages.deltas.method.MethodDelta
import miksilo.modularLanguages.deltas.statement.BlockDelta

import scala.collection.mutable

object JavaClassDelta extends DeltaWithGrammar with Delta
  with HasDeclarationDelta with HasConstraintsDelta {

  import miksilo.modularLanguages.deltas.HasNameDelta._

  override def shape: NodeShape = ClassDelta.Shape

  override def description: String = "Defines a skeleton for the Java class."

  def getFields[T <: NodeLike](javaClass: JavaClass[T]): Seq[T] = {
    javaClass.members.filter(member => member.shape == Shape)
  }

  @scala.annotation.tailrec
  def fullyQualify(_type: NodePath, classCompiler: ClassCompiler): Unit =  _type.shape match {
    case ArrayTypeDelta.Shape => fullyQualify(ArrayTypeDelta.getElementType(_type), classCompiler)
    case UnqualifiedObjectTypeDelta.Shape =>
        val newName = classCompiler.fullyQualify(UnqualifiedObjectTypeDelta.getName(_type))
      _type.asInstanceOf[NodeChildPath].replaceWith(QualifiedObjectTypeDelta.neww(newName))
    case _ =>
  }

  def getClassCompiler(compilation: Compilation): ClassCompiler = state(compilation).classCompiler

  def getQualifiedClassName[T <: NodeLike](javaClass: JavaClass[T]): QualifiedClassName = {
    QualifiedClassName(javaClass._package ++ Seq(javaClass.name))
  }

  override def dependencies: Set[Contract] = Set(BlockDelta, MethodDelta, FieldDeclarationDelta)

  override def transformGrammars(grammars: LanguageGrammars, language: Language): Unit = {
    import grammars._

    val classMember: BiGrammar = find(MethodDelta.Shape) | find(FieldDeclarationDelta.Shape)
    val importGrammar = create(ImportGrammar)
    val importsGrammar: BiGrammar = importGrammar.manyVertical as ClassImports
    val packageGrammar = new BiChoice(keywordGrammar("package") ~~>
      identifier.someSeparated(".") ~< ";", value(Seq.empty), true) as ClassPackage
    val classParentGrammar = ("extends" ~~> identifier).option
    val nameGrammar: BiGrammar = "class" ~~> find(Name)
    val membersGrammar = "{" % (classMember.manySeparatedVertical(BlankLine) as Members).indent(BlockDelta.indentAmount) % "}"
    val nameAndParent: BiGrammar = nameGrammar ~~ classParentGrammar.as(ClassParent)
    val classGrammar = packageGrammar % importsGrammar % nameAndParent % membersGrammar asLabelledNode Shape
    find(BodyGrammar).inner = classGrammar
  }


  object ImportGrammar extends GrammarKey

  def neww(_package: Seq[String], name: String, members: Seq[Node] = Seq(), imports: List[Node] = List(), mbParent: Option[String] = None) =
    new Node(Shape,
    Members -> members,
    ClassPackage -> _package,
    Name -> name,
    ClassImports -> imports,
    ClassParent -> mbParent)

  val importToClassMap = new ShapeProperty[(Compilation, Node) => Map[String, QualifiedClassName]]

  val state = new CompilationField[State](_ => new State())
  class State {
    var classCompiler: ClassCompiler = _
    val javaCompiler: JavaCompiler = new JavaCompiler()
    var packageScopes: mutable.Map[String, Scope] = new mutable.HashMap
  }

  object ClassGrammar

  override def collectConstraints(compilation: Compilation, builder: ConstraintBuilder, path: NodePath, defaultPackageScope: Scope): Unit = {
    getClassScope(compilation, builder, path, defaultPackageScope)
  }

  def getClassScope(compilation: Compilation, builder: ConstraintBuilder, path: NodePath, defaultPackageScope: Scope): ScopeVariable = {
    val clazz: JavaClass[NodePath] = path
    val clazzDeclaration = getDeclaration(compilation, builder, clazz.node, defaultPackageScope)
    val classScope = builder.getDeclaredScope(clazzDeclaration)
    for (_import <- clazz.imports)
      ConstraintSkeleton.constraints(compilation, builder, _import, classScope)

    val members = clazz.members

    members.foreach(member =>
      ConstraintSkeleton.constraints(compilation, builder, member, classScope))

    classScope
  }

  val staticDeclaration = new TypedNodeField[NamedDeclaration]("staticDeclaration")
  override def getDeclaration(compilation: Compilation, builder: ConstraintBuilder, path: NodePath, defaultPackageScope: Scope): Declaration = {
    val clazz: JavaClass[NodePath] = path

    val packageScope = if (clazz._package.isEmpty) {
      defaultPackageScope
    } else {
      val packageParts = clazz.node._package.toList
      val fullPackage: String = packageParts.reduce[String]((a, b) => a + "." + b)
      state(compilation).packageScopes.getOrElseUpdate(fullPackage, {
        val packageDeclaration = builder.declare(fullPackage, defaultPackageScope, path)
        builder.declareScope(packageDeclaration, defaultPackageScope, fullPackage)
      })
    }

    //TODO here there should be an instance, a static, and a lexical scope.
    val clazzDeclaration = builder.declare(clazz.name, packageScope, path.getField(Name))
    val clazzType = TypeFromDeclaration(clazzDeclaration)
    builder.add(DeclarationHasType(clazzDeclaration, clazzType))
    builder.assignSubType(TypeSkeleton.typeKind, clazzType)

    val classScope = builder.declareScope(clazzDeclaration, packageScope, clazz.name)
    staticDeclaration(path) = clazzDeclaration

    val members = clazz.members
    members.foreach(member => ConstraintSkeleton.hasDeclarations(compilation, member.shape).
      getDeclaration(compilation, builder, member, classScope))

    clazzDeclaration
  }
}
