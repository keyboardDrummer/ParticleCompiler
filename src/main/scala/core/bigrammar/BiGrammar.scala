package core.bigrammar

import core.bigrammar.grammars._
import core.document.WhiteSpace
import core.deltas.node.GrammarKey
import util.{GraphBasics, Utility}

import scala.reflect.ClassTag

object BiGrammar {
  type State = Map[Any, Any]
}

/*
A grammar that maps to both a parser and a printer
 */
trait BiGrammar extends BiGrammarWriter {

  override def toString: String = PrintBiGrammar.toDocument(this).renderString(trim = false)

  lazy val height = 1

  def |(other: BiGrammar) = new Choice(this, other)
  def option: BiGrammar = this.mapSome[Any, Option[Any]](x => Some(x), x => x) | value(None)

  def indent(width: Int = 2): BiGrammar = new LeftRight(WhiteSpace(width, 0), this).ignoreLeft

  def optionToSeq: BiGrammar = this.map[Option[Any], Seq[Any]](
    option => option.fold[Seq[Any]](Seq.empty)(v => Seq(v)),
    seq => if (seq.isEmpty) None else Some(seq))

  def seqToSet: BiGrammar = this.map[Seq[Any], Set[Any]](
    seq => seq.toSet,
    set => set.toSeq)

  def map[T, U: ClassTag](afterParsing: T => U, beforePrinting: U => T): BiGrammar =
    mapSome(afterParsing, (u: U) => Some(beforePrinting(u)))

  def mapSome[T, U: ClassTag](afterParsing: T => U, beforePrinting: U => Option[T]): BiGrammar =
    new MapGrammar(this, value => afterParsing(value.asInstanceOf[T]),
      value => Utility.cast[U](value).flatMap(value => beforePrinting(value)))

  def children: Seq[BiGrammar]
  def withChildren(newChildren: Seq[BiGrammar]): BiGrammar
  def deepMap(function: BiGrammar => BiGrammar): BiGrammar = new BiGrammarObserver[BiGrammar] {
    override def getReference(name: GrammarKey): BiGrammar = new Labelled(name)

    override def setReference(result: BiGrammar, reference: BiGrammar): Unit = {
      reference.asInstanceOf[Labelled].inner = result.asInstanceOf[Labelled].inner
    }

    override def handleGrammar(self: BiGrammar, children: Seq[BiGrammar], recursive: (BiGrammar) => BiGrammar): BiGrammar = self.withChildren(children)
  }.observe(this)

  def deepClone: BiGrammar = deepMap(x => x)
  def containsParser(): Boolean = {
    var map: Map[BiGrammar, Boolean] = Map.empty
    lazy val recursive: BiGrammar => Boolean = grammar => {
      map.get(grammar) match {
        case Some(result) => result
        case _ =>
          map += grammar -> false
          val result = grammar.containsParser(recursive)
          map += grammar -> result
          result
      }
    }
    this.containsParser(recursive)
  }

  protected def containsParser(recursive: BiGrammar => Boolean): Boolean

  def selfAndDescendants: Seq[BiGrammar] = GraphBasics.traverseBreadth[BiGrammar](Seq(this), grammar => grammar.children)
}
