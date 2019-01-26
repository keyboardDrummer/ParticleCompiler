package core.smarts

import com.typesafe.scalalogging.LazyLogging
import core.deltas.path.ChildPath
import core.deltas.{Contract, Delta}
import core.language.exceptions.BadInputException
import core.language.node.FieldExtension
import core.language.{Language, Phase}
import core.smarts.objects.Declaration

import scala.util.{Failure, Success}

object SolveConstraintsDelta extends Delta with LazyLogging {

  val referenceDeclaration = new FieldExtension[Declaration]()
  override def inject(language: Language): Unit = {
    super.inject(language)
    language.compilerPhases ::= Phase(this, compilation => {
      val factory = new Factory()
      val builder = new ConstraintBuilder(factory)
      language.collectConstraints(compilation, builder)

      val solver = builder.toSolver

      solver.run() match {
        case Success(_) =>
          compilation.remainingConstraints = Seq.empty
        case Failure(e:CouldNotApplyConstraints) =>
          compilation.remainingConstraints = e.constraints
        case Failure(e:SolveException) =>
          throw ConstraintException(e)
        case Failure(e) => throw e
      }

      for(refDecl <- solver.proofs.declarations) {
        refDecl._1.origin.foreach(ref => referenceDeclaration(ref.asInstanceOf[ChildPath]) = refDecl._2)
      }
      compilation.proofs = solver.proofs
      compilation.diagnostics ++= compilation.remainingConstraints.flatMap(
        constraint => constraint.getDiagnostic.toSeq)
    })
  }

  case class ConstraintException(solveException: SolveException) extends BadInputException {
    override def toString: String = "Could not solve semantic constraints:" + solveException.toString
  }

  override def description: String = "Solves the semantic constraints"

  override def dependencies: Set[Contract] = Set.empty
}
