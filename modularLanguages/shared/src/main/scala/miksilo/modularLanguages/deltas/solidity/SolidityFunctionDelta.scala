package miksilo.modularLanguages.deltas.solidity

import miksilo.modularLanguages.core.bigrammar.BiGrammar
import miksilo.modularLanguages.core.deltas.DeltaWithGrammar
import miksilo.modularLanguages.core.deltas.grammars.LanguageGrammars
import miksilo.modularLanguages.core.deltas.path.NodePath
import miksilo.modularLanguages.core.node.{Node, NodeField, NodeWrapper}
import miksilo.languageServer.core.language.{Compilation, Language}
import miksilo.languageServer.core.smarts.ConstraintBuilder
import miksilo.languageServer.core.smarts.scopes.objects.Scope
import miksilo.modularLanguages.deltas.ConstraintSkeleton
import miksilo.modularLanguages.deltas.HasNameDelta.Name
import miksilo.modularLanguages.deltas.bytecode.types.TypeSkeleton
import miksilo.modularLanguages.deltas.classes.ClassDelta
import miksilo.modularLanguages.deltas.classes.HasConstraintsDelta
import miksilo.modularLanguages.deltas.javac.methods.MethodParameters.MethodParameter
import miksilo.modularLanguages.deltas.javac.methods.{MethodParameters}
import miksilo.modularLanguages.deltas.method.MethodDelta
import miksilo.modularLanguages.deltas.method.MethodDelta.Method
import miksilo.modularLanguages.deltas.method.call.CallDelta
import miksilo.modularLanguages.deltas.statement.{BlockDelta, LabelStatementDelta}

object SolidityFunctionDelta extends DeltaWithGrammar with HasConstraintsDelta {

  object ReturnValues extends NodeField
  object Modifiers extends NodeField

  object ParameterStorageLocation extends NodeField

  override def transformGrammars(grammars: LanguageGrammars, language: Language): Unit = {
    import grammars._
    val typeGrammar = find(TypeSkeleton.JavaTypeGrammar)
    val storageLocation: BiGrammar = find(StorageLocationDelta.StorageLocation)
    val parameter = typeGrammar.as(MethodParameters.Type) ~
      storageLocation ~~
      find(Name) asNode MethodParameters.Shape
    val parameterList = create(MethodDelta.Parameters, parameter.toParameterList)

    val returnParameter = typeGrammar.as(MethodParameters.Type) ~
      storageLocation ~
      identifier.spacedOption.as(Name) asNode MethodParameters.Shape
    val returnParameterList = returnParameter.toParameterList

    val name = (identifier | value("<default>")).as(Name)

    val modifierInvocation = find(Name) ~
      (find(CallDelta.CallArgumentsGrammar) | value(Seq.empty)).as(CallDelta.Arguments) asNode CallDelta.Shape

    val stateMutability = find(StateMutabilityDelta.Grammar)
    val modifiers = create(Modifiers, (printSpace ~> (modifierInvocation | stateMutability | "external" | "public" | "internal" | "private")).many.as(Modifiers))
    val returnValues = (printSpace ~ "returns" ~~> returnParameterList | value(Seq.empty)).as(ReturnValues)
    val blockGrammar: BiGrammar = find(BlockDelta.BlockGrammar)
    val body = (";" ~> value(BlockDelta.neww(Seq.empty)) | blockGrammar).as(MethodDelta.Body)
    val grammar = "function" ~~ name ~ parameterList.as(MethodDelta.Parameters) ~ modifiers ~ returnValues ~~ body asLabelledNode MethodDelta.Shape
    find(ClassDelta.Members).addAlternative(grammar)
  }

  override def inject(language: Language): Unit = {
    LabelStatementDelta.isLabelScope.add(language, MethodDelta.Shape, ())
    super.inject(language)
  }

  override def description = "Adds solidity functions"

  override def dependencies = Set(TypeSkeleton, BlockDelta, StorageLocationDelta)

  override def shape = MethodDelta.Shape

  override def collectConstraints(compilation: Compilation, builder: ConstraintBuilder, path: NodePath, parentScope: Scope): Unit = {
    val method: Method[NodePath] = path

    val parameterTypes = method.parameters.map(p => p(MethodParameters.Type).asInstanceOf[NodePath])
    val returnParameters: Seq[MethodParameter[NodePath]] = NodeWrapper.wrapList(method(ReturnValues).asInstanceOf[Seq[NodePath]])
    val returnTypes: Seq[Node] = returnParameters.map(returnParameter => returnParameter._type)
    val methodType = SolidityFunctionTypeDelta.createType(compilation, builder, parentScope, parameterTypes, returnTypes)

    builder.declare(method.name, parentScope, path.getField(Name), Some(methodType))

    val bodyScope = builder.newScope(parentScope)
    method.parameters.foreach(parameter => {
      MethodParameters.declare(compilation, builder, parameter, parentScope, bodyScope)
    })

    val returnValues = NodeWrapper.wrapList[MethodParameter[NodePath], NodePath](method(ReturnValues).asInstanceOf[Seq[NodePath]])
    returnValues.foreach(parameter => {
      val maybeName = parameter.getValue(Name).asInstanceOf[Option[String]]
      maybeName.foreach(name => {
        val parameterType = TypeSkeleton.getType(compilation, builder, parameter._type, parentScope)
        builder.declare(name, bodyScope, parameter.getField(Name), Some(parameterType))
      })
    })
    ConstraintSkeleton.constraints(compilation, builder, method.body, bodyScope)
  }
}


