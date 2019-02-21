package deltas.javac.classes.skeleton

import core.bigrammar.BiGrammar
import core.deltas._
import core.deltas.grammars.{BodyGrammar, LanguageGrammars}
import core.deltas.path.{NodeChildPath, NodePath, PathRoot}
import core.document.BlankLine
import core.language.node._
import core.language.{Compilation, CompilationState, Language}
import core.smarts.ConstraintBuilder
import core.smarts.objects.{Declaration, NamedDeclaration}
import core.smarts.scopes.objects.{Scope, ScopeVariable}
import core.smarts.types.DeclarationHasType
import core.smarts.types.objects.TypeFromDeclaration
import deltas.ConstraintSkeleton
import deltas.bytecode.types.{ArrayTypeDelta, QualifiedObjectTypeDelta, TypeSkeleton, UnqualifiedObjectTypeDelta}
import deltas.javac.JavaLang
import deltas.javac.classes.{ClassCompiler, FieldDeclarationDelta}
import deltas.javac.methods.MethodDelta
import deltas.statement.BlockDelta

import scala.collection.mutable

object JavaClassDelta extends DeltaWithGrammar with Delta
  with HasDeclarationDelta with HasConstraintsDelta {

  import deltas.HasNameDelta._

  override def shape: NodeShape = Shape

  override def description: String = "Defines a skeleton for the Java class."

  implicit class JavaClass[T <: NodeLike](val node: T) extends HasName[T] {
    def _package: Seq[String] = node(ClassPackage).asInstanceOf[Seq[String]]
    def _package_=(value: Seq[String]): Unit = node(ClassPackage) = value

    def imports = node(ClassImports).asInstanceOf[Seq[T]]
    def imports_=(value: Seq[T]): Unit = node(ClassImports) = value

    def members = node(Members).asInstanceOf[Seq[T]]
    def members_=(value: Seq[T]): Unit = node(Members) = value

    def parent: Option[String] = node.getValue(ClassParent).asInstanceOf[Option[String]]
    def parent_=(value: Option[String]): Unit = node(ClassParent) = value
  }

  def getFields[T <: NodeLike](javaClass: JavaClass[T]): Seq[T] = {
    javaClass.members.filter(member => member.shape == Shape)
  }

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
    import language.grammars._

    val classMember: BiGrammar = find(MethodDelta.Shape) | find(FieldDeclarationDelta.Shape)
    val importGrammar = create(ImportGrammar)
    val importsGrammar: BiGrammar = importGrammar.manyVertical as ClassImports
    val packageGrammar = (keyword("package") ~~> identifier.someSeparated(".") ~< ";") | value(Seq.empty) as ClassPackage
    val classParentGrammar = ("extends" ~~> identifier).option
    val nameGrammar: BiGrammar = "class" ~~> find(Name)
    val membersGrammar = "{".%((classMember.manySeparatedVertical(BlankLine) as Members).indent(BlockDelta.indentAmount)) % "}"
    val nameAndParent: BiGrammar = nameGrammar ~~ classParentGrammar.as(ClassParent)
    val classGrammar = packageGrammar % importsGrammar % nameAndParent % membersGrammar asLabelledNode Shape
    find(BodyGrammar).inner = classGrammar
  }


  object ImportGrammar extends GrammarKey
  object Shape extends NodeShape

  def neww(_package: Seq[String], name: String, members: Seq[Node] = Seq(), imports: List[Node] = List(), mbParent: Option[String] = None) =
    new Node(Shape,
    Members -> members,
    ClassPackage -> _package,
    Name -> name,
    ClassImports -> imports,
    ClassParent -> mbParent)

  val importToClassMap = new ShapeProperty[(Compilation, Node) => Map[String, QualifiedClassName]]

  val state = new CompilationState[State](new State())
  class State {
    var classCompiler: ClassCompiler = _
    val javaCompiler: JavaCompiler = new JavaCompiler()
    var packageScopes: mutable.Map[String, Scope] = mutable.Map.empty
  }

  object ClassGrammar

  object ClassPackage extends NodeField

  object ClassImports extends NodeField

  object ClassParent extends NodeField

  object Members extends NodeField

  override def inject(language: Language): Unit = {

    language.collectConstraints = (compilation, builder) => {
      val defaultPackageScope = builder.newScope(None, "defaultPackageScope")
      val proofs = JavaLang.getProofs(compilation, builder.factory, defaultPackageScope)
      builder.proofs = proofs

      ConstraintSkeleton.constraints(
        compilation, builder, PathRoot(compilation.program), defaultPackageScope)
    }
    super.inject(language)
  }

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
        builder.declareScope(packageDeclaration, Some(defaultPackageScope), fullPackage )
      })
    }

    //TODO here there should be an instance, a static, and a lexical scope.
    val clazzDeclaration = builder.declare(clazz.name, packageScope, path.getSourceElement(Name))
    val clazzType = TypeFromDeclaration(clazzDeclaration)
    builder.add(DeclarationHasType(clazzDeclaration, clazzType))
    builder.assignSubType(TypeSkeleton.typeKind, clazzType)

    val classScope = builder.declareScope(clazzDeclaration, Some(packageScope), clazz.name)
    staticDeclaration(path) = clazzDeclaration

    val members = clazz.members
    members.foreach(member => ConstraintSkeleton.hasDeclarations(compilation, member.shape).
      getDeclaration(compilation, builder, member, classScope))

    clazzDeclaration
  }
}
