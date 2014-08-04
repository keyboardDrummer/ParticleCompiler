package core.exceptions

import core.transformation.Contract

case class TransformationDependencyViolation(dependency: Contract, dependent: Contract) extends CompilerException {
  override def toString = s"dependency '${dependency.name}' from '${dependent.name}' is not satisfied"
}
