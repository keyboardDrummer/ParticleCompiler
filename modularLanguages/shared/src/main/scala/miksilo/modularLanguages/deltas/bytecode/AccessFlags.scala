package miksilo.modularLanguages.deltas.bytecode

import miksilo.modularLanguages.core.node.{Key, Node, NodeField}
import miksilo.modularLanguages.deltas.bytecode.PrintByteCode._

trait AccessFlags {
  trait MethodAccessFlag extends Key

  object PublicAccess extends MethodAccessFlag

  object StaticAccess extends MethodAccessFlag

  object PrivateAccess extends MethodAccessFlag

  object AccessFlagsKey extends NodeField

  private val accessCodesToByteCode = Map(
    PublicAccess -> "0001",
    StaticAccess -> "0008",
    PrivateAccess -> "0002").view.mapValues(s => hexToInt(s)).toMap

  def getAccessFlagsByteCode(field: Node): Seq[Byte] = {
    getAccessFlagsByteCode(getAccessFlags(field))
  }

  private def getAccessFlagsByteCode(accessFlags: Set[MethodAccessFlag]): Seq[Byte] = {
    shortToBytes(accessFlags.map(flag => accessCodesToByteCode(flag)).sum)
  }

  private def getAccessFlags(_object: Node): Set[MethodAccessFlag] = {
    _object(AccessFlagsKey).asInstanceOf[Set[MethodAccessFlag]]
  }
}
