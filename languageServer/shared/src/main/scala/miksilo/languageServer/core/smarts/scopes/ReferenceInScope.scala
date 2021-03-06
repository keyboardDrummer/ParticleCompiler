package miksilo.languageServer.core.smarts.scopes

import miksilo.languageServer.core.smarts.ConstraintSolver
import miksilo.languageServer.core.smarts.scopes.objects.{ConcreteScope, Scope, ScopeVariable}
import miksilo.languageServer.core.smarts.objects.Reference

case class ReferenceInScope(reference: Reference, var scope: Scope) extends ScopeConstraint {
  override def instantiateScope(variable: ScopeVariable, instance: Scope): Unit = {
    if (scope == variable)
      scope = instance
  }

  override def apply(solver: ConstraintSolver): Boolean = scope match {
    case concrete: ConcreteScope => solver.scopeGraph.addEdge(reference, ReferenceEdge(concrete)); true
    case _ => false
  }
}
