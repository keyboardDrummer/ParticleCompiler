package deltas.verilog

import core.bigrammar.BiGrammar
import core.deltas.grammars.LanguageGrammars
import core.deltas.path.NodePath
import core.deltas.{Contract, DeltaWithGrammar}
import core.language.node._
import core.language.{Compilation, Language}
import core.smarts.ConstraintBuilder
import core.smarts.scopes.objects.Scope
import deltas.{ConstraintSkeleton, FileWithMembersDelta}
import deltas.HasNameDelta.HasName
import deltas.expression.VariableDelta
import deltas.expression.VariableDelta.Variable
import deltas.javac.classes.skeleton.HasConstraintsDelta

object VerilogModuleDelta extends DeltaWithGrammar with HasConstraintsDelta {

  import deltas.HasNameDelta.Name
  object Shape extends NodeShape

  object Ports extends NodeField
  object Body extends NodeField

  object MemberShape extends NodeShape

  def neww(name: String, ports: Seq[Node], body: Seq[Node]): Node = Shape.create(
    Name -> name,
    Ports -> ports,
    Body -> body)

  implicit class Module[T <: NodeLike](val node: T) extends NodeWrapper[T] with HasName[T] {
    def ports: Seq[Variable[T]] = NodeWrapper.wrapList(node(Ports).asInstanceOf[Seq[T]])
    def body: Seq[T] = node(Body).asInstanceOf[Seq[T]]
  }

  override def transformGrammars(grammars: LanguageGrammars, language: Language): Unit = {
    import grammars._

    val member = create(MemberShape)
    val variable = find(VariableDelta.Shape)
    val parameterList: BiGrammar = (variable.manySeparatedVertical(",").inParenthesis | value(Seq.empty)).as(Ports)
    val body: BiGrammar = member.manyVertical.as(Body)
    val moduleGrammar: BiGrammar = "module" ~~ find(Name) ~~ parameterList ~ ";" % body.indent() % "endmodule" asNode Shape
    find(FileWithMembersDelta.Members).addAlternative(moduleGrammar)
  }

  override def description: String = "Adds the Verilog module"

  override def collectConstraints(compilation: Compilation, builder: ConstraintBuilder, path: NodePath, parentScope: Scope): Unit = {
    val moduleScope = builder.newScope(Some(parentScope), "moduleScope")
    val module: Module[NodePath] = path
    for(port <- module.ports) {
      builder.declare(port.name, moduleScope, port, None)
    }

    for(member <- module.body) {
      ConstraintSkeleton.constraints(compilation, builder, member, moduleScope)
    }
  }

  override def shape: NodeShape = Shape
  override def dependencies: Set[Contract] = Set(FileWithMembersDelta, VariableDelta)
}


