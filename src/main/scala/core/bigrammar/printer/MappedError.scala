package core.bigrammar.printer

import core.responsiveDocument.ResponsiveDocument

case class MappedError(f: ResponsiveDocument => ResponsiveDocument, inner: PrintError) extends PrintError
{
  override def toDocument = inner.toDocument

  override def partial = f(inner.partial)

  override val depth = 1 + inner.depth
}
