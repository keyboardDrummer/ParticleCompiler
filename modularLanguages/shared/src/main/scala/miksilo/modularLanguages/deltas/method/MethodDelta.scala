package miksilo.modularLanguages.deltas.method

import miksilo.modularLanguages.core.bigrammar.BiGrammar
import miksilo.modularLanguages.core.deltas._
import miksilo.modularLanguages.core.deltas.grammars.LanguageGrammars
import miksilo.modularLanguages.core.deltas.path.{NodePath, PathRoot}
import miksilo.modularLanguages.core.node._
import miksilo.languageServer.core.language.{Compilation, CompilationField, Language}
import miksilo.languageServer.core.smarts.ConstraintBuilder
import miksilo.languageServer.core.smarts.objects.Declaration
import miksilo.languageServer.core.smarts.scopes.objects.{ConcreteScope, Scope}
import miksilo.modularLanguages.deltas.ConstraintSkeleton
import miksilo.modularLanguages.deltas.bytecode.extraConstants.TypeConstant
import miksilo.modularLanguages.deltas.bytecode.types.{TypeSkeleton, VoidTypeDelta}
import miksilo.modularLanguages.deltas.classes.ClassDelta.JavaClass
import miksilo.modularLanguages.deltas.classes.HasConstraintsDelta
import miksilo.modularLanguages.deltas.javac.classes.{ClassCompiler, MethodInfo}
import miksilo.modularLanguages.deltas.javac.classes.skeleton.{ClassSignature, HasDeclarationDelta, JavaClassDelta, MethodClassKey}
import miksilo.modularLanguages.deltas.javac.methods.{AccessibilityFieldsDelta, MethodCompiler, MethodParameters}
import miksilo.modularLanguages.deltas.javac.methods.AccessibilityFieldsDelta.{HasAccessibility, PrivateVisibility}
import miksilo.modularLanguages.deltas.javac.methods.MethodParameters.MethodParameter
import miksilo.modularLanguages.deltas.javac.types.{MethodTypeDelta, TypeAbstraction}
import miksilo.modularLanguages.deltas.statement.{BlockDelta, LabelStatementDelta}
import miksilo.modularLanguages.deltas.statement.BlockDelta.BlockStatement

object MethodDelta extends DeltaWithGrammar
  with HasDeclarationDelta with HasConstraintsDelta with HasShape {

  import miksilo.modularLanguages.deltas.HasNameDelta._

  override def description: String = "Enables Java classes to contain methods."

  implicit class Method[T <: NodeLike](val node: T) extends HasAccessibility[T] with HasName[T] {

    def returnType: T = node(ReturnType).asInstanceOf[T]
    def returnType_=(value: T): Unit = node(ReturnType) = value

    def parameters: Seq[MethodParameter[T]] = NodeWrapper.wrapList(node(Parameters).asInstanceOf[Seq[T]])
    def parameters_=(value: Seq[MethodParameter[T]]): Unit = node(Parameters) = NodeWrapper.unwrapList(value)

    def body: BlockStatement[T] = node(Body).asInstanceOf[T]
  }

  def bind(compilation: Compilation, signature: ClassSignature, method: Method[Node]): Unit = {
    val classCompiler = JavaClassDelta.getClassCompiler(compilation)
    val classInfo = classCompiler.currentClassInfo

    val methodName: String = MethodDelta.getMethodName(method)
    val parameters = method.parameters
    val parameterTypes = parameters.map(p => getParameterType(PathRoot(p), classCompiler))
    val _type = getMethodType(method)
    val key = MethodClassKey(methodName, parameterTypes.toVector)
    classInfo.methods(key) = MethodInfo(_type, method.isStatic)
  }

  def getMethodType[T <: NodeLike](method: Method[T]) = {
    val parameterTypes = method.parameters.map(p => p(MethodParameters.Type).asInstanceOf[T].asNode)
    MethodTypeDelta.neww(method.returnType.asNode, parameterTypes)
  }

  override def dependencies: Set[Contract] = Set(BlockDelta, AccessibilityFieldsDelta)

  def getParameterType(parameter: MethodParameter[NodePath], classCompiler: ClassCompiler): Node = {
    val result = parameter._type
    result
  }

  def getMethodDescriptor(method: Method[Node], classCompiler: ClassCompiler): Node = {
    TypeConstant.constructor(getMethodType(method))
  }


  def setMethodCompiler(method: Node, compilation: Compilation): Unit = {
    state(compilation) = MethodCompiler(compilation, method)
  }

  def getMethodCompiler(compilation: Compilation) = state(compilation)

  def getMethodName(method: Node) = {
    method(Name).asInstanceOf[String]
  }

  def getMethods[T <: NodeLike](javaClass: JavaClass[T]): Seq[Method[T]] =
    NodeWrapper.wrapList(javaClass.members.filter(member => member.shape == Shape))

  object ReturnTypeGrammar extends GrammarKey

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit =  {
    val _grammars = grammars
    import grammars._
    val block = find(BlockDelta.BlockGrammar)

    val parseType = find(TypeSkeleton.JavaTypeGrammar)
    val parseReturnType = create(ReturnTypeGrammar, parseType)

    val parseParameter = MethodParameters.getGrammar(_grammars)
    val parseParameters = create(Parameters, "(" ~> parseParameter.manySeparated(",") ~< ")")

    val typeParametersGrammar: BiGrammar = find(TypeAbstraction.TypeParametersGrammar)

    val methodUnmapped: BiGrammar = find(AccessibilityFieldsDelta.VisibilityField) ~
      find(AccessibilityFieldsDelta.Static) ~ typeParametersGrammar.as(TypeParameters) ~
      parseReturnType.as(ReturnType) ~~ find(Name) ~ parseParameters.as(Parameters) % block.as(Body)
    create(Shape, methodUnmapped.asNode(Shape))
  }

  def neww(name: String, _returnType: Any, _parameters: Seq[Node], _body: Node,
           static: Boolean = false,
           visibility: AccessibilityFieldsDelta.Visibility = PrivateVisibility,
           typeParameters: Seq[Node] = Seq.empty): Node = {
    new Node(Shape,
      Name -> name,
      ReturnType -> _returnType,
      Parameters -> _parameters,
      Body -> _body,
      AccessibilityFieldsDelta.Static -> static,
      AccessibilityFieldsDelta.VisibilityField -> visibility,
      TypeParameters -> typeParameters)
  }

  val state = new CompilationField[MethodCompiler](null)

  object Shape extends NodeShape {
    override def toString: String = "Method"
  }

  object Body extends NodeField {
    override def toString: String = "Body"
  }

  object ReturnType extends NodeField {
    override def toString: String = "ReturnType"
  }

  object Parameters extends NodeField {
    override def toString: String = "Parameters"
  }

  object TypeParameters extends NodeField {
    override def toString: String = "TypeParameters"
  }

  override def getDeclaration(compilation: Compilation, builder: ConstraintBuilder, path: NodePath, parentScope: Scope): Declaration = {
    val method: Method[NodePath] = path
    val parameterTypes = method.parameters.map(p => p(MethodParameters.Type).asInstanceOf[NodePath])
    val returnType = method.returnType
    val methodType = MethodTypeDelta.getType(compilation, builder, parentScope, parameterTypes, returnType)

    builder.declare(method.name, parentScope, path.getField(Name), Some(methodType))
  }

  override def collectConstraints(compilation: Compilation, builder: ConstraintBuilder, path: NodePath, parentScope: Scope): Unit = {
    getBodyScope(compilation, builder, path, parentScope)
  }

  def getBodyScope(compilation: Compilation, builder: ConstraintBuilder, path: NodePath, parentScope: Scope): ConcreteScope = {
    val method: Method[NodePath] = path
    val bodyScope = builder.newScope(parentScope, "methodBody")
    method.parameters.foreach(parameter => {
      MethodParameters.declare(compilation, builder, parameter, parentScope, bodyScope)
    })
    ConstraintSkeleton.constraints(compilation, builder, method.body, bodyScope)
    bodyScope
  }

  override def shape: NodeShape = Shape

  override def inject(language: Language): Unit = {
    LabelStatementDelta.isLabelScope.add(language, Shape, ())
    super.inject(language)
  }
}
