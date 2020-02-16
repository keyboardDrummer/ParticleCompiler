package core.smarts.objects

import core.language.SourceElement
import core.smarts.scopes.GraphNode

//TODO indicate that Reference may not be a case class.
//TODO Maybe refs should have an optional origin, in case of implicit refs.
class Reference(val name: String, val origin: Option[SourceElement]) extends GraphNode
{
  override def toString = s"Reference($name, $origin)"
}