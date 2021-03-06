package miksilo.modularLanguages.core.bigrammar

import miksilo.modularLanguages.core.bigrammar.grammars.{BiChoice, BiSequence, Labelled, ValueMapGrammar}
import miksilo.editorParser.document.WhiteSpace
import miksilo.languageServer.util.{GraphBasics, Utility}
import miksilo.modularLanguages.core.node.GrammarKey

import scala.reflect.ClassTag

object BiGrammar {
  type State = Map[Any, Any]
}

/*
A grammar that maps to both a parser and a printer
 */
trait BiGrammar {
  import BiGrammarWriter._

  override def toString: String = PrintBiGrammar.toDocument(this).renderString(trim = false)

  lazy val height = 1

  def |(other: BiGrammar) = new BiChoice(this, other, !other.containsParser())
  def option: BiGrammar = new BiChoice(this.mapSome[Any, Option[Any]](x => Some(x), x => x), value(None), true)

  def indent(width: Int = 2): BiGrammar =
    leftRight(WhiteSpace(width, 0), this, BiSequence.ignoreLeft)

  def flattenOptionSeq: BiGrammar = this.map[Option[Seq[Any]], Seq[Any]](
    option => option.fold[Seq[Any]](Seq.empty)(v => v),
    seq => if (seq.isEmpty) None else Some(seq))

  def optionToSeq: BiGrammar = this.map[Option[Any], Seq[Any]](
    option => option.fold[Seq[Any]](Seq.empty)(v => Seq(v)),
    seq => if (seq.isEmpty) None else Some(seq))

  def seqToSet: BiGrammar = this.map[Seq[Any], Set[Any]](
    seq => seq.toSet,
    set => set.toSeq)

  def setValue[T: ClassTag](value: T): BiGrammar = new ValueMapGrammar[Any, T](this, _ => Right(value),
    aValue => if (aValue == value) Some(()) else None)

  def map[T, U: ClassTag](afterParsing: T => U, beforePrinting: U => T): BiGrammar =
    mapSome(afterParsing, (u: U) => Some(beforePrinting(u)))

  def mapSome[T, U: ClassTag](afterParsing: T => U, beforePrinting: U => Option[T]): BiGrammar =
    new ValueMapGrammar[T, U](this,
      value => Right(afterParsing(value)),
      value => Utility.cast[U](value).flatMap(value => beforePrinting(value)))

  def children: Seq[BiGrammar]
  def withChildren(newChildren: Seq[BiGrammar]): BiGrammar
  def deepMap(function: BiGrammar => BiGrammar): BiGrammar = new BiGrammarObserver[BiGrammar] {
    override def getReference(name: GrammarKey): BiGrammar = new Labelled(name)

    override def setReference(result: BiGrammar, reference: BiGrammar): Unit = {
      reference.asInstanceOf[Labelled].inner = result.asInstanceOf[Labelled].inner
    }

    override def handleGrammar(self: BiGrammar, children: Seq[BiGrammar], recursive: BiGrammar => BiGrammar): BiGrammar = self.withChildren(children)
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

  def isLeftRecursive: Boolean = {
    var isRecursive = false
    var seen = Set.empty[BiGrammar]
    lazy val recursive: BiGrammar => Seq[BiGrammar] = grammar => {
      if (grammar == this || isRecursive) {
        isRecursive = true
        Seq.empty
      }
      else {
        if (!seen.contains(grammar)) {
          seen += grammar
          grammar.getLeftChildren(recursive)
        } else
          Seq.empty
      }
    }
    this.getLeftChildren(recursive)
    isRecursive
  }

  def getLeftChildren: Seq[BiGrammar] = {
    var map: Map[BiGrammar, Seq[BiGrammar]] = Map.empty
    lazy val recursive: BiGrammar => Seq[BiGrammar] = grammar => {
      map.get(grammar) match {
        case Some(result) => result
        case _ =>
          map += grammar -> Seq.empty
          val result = Seq(grammar) ++ grammar.getLeftChildren(recursive)
          map += grammar -> result
          result
      }
    }
    Seq(this) ++ this.getLeftChildren(recursive)
  }

  protected def getLeftChildren(recursive: BiGrammar => Seq[BiGrammar]): Seq[BiGrammar] =
    children.flatMap(c => recursive(c))

  def containsParser(recursive: BiGrammar => Boolean): Boolean

  def selfAndDescendants: Seq[BiGrammar] = GraphBasics.traverseBreadth[BiGrammar](Seq(this), grammar => grammar.children)
}
