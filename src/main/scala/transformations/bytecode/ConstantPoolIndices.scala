package transformations.bytecode

import core.bigrammar._
import core.particles.grammars.GrammarCatalogue
import core.particles.node.{Node, NodeClass, NodeField}
import core.particles.{Language, DeltaWithGrammar}
import transformations.bytecode.ByteCodeSkeleton.ConstantPoolGrammar

object ConstantPoolIndices extends DeltaWithGrammar {

  private object WithIndexClass extends NodeClass

  private object Index extends NodeField

  private object Content extends NodeField

  override def transformGrammars(grammars: GrammarCatalogue, state: Language): Unit = {
    val previousConstantPoolItem = grammars.find(ByteCodeSkeleton.ConstantPoolItemContentGrammar)
    val constantPoolItem = (("#" ~> number.as(Index) <~~ "=") ~~ previousConstantPoolItem.inner.as(Content)).
      asNode(WithIndexClass)
    previousConstantPoolItem.inner = constantPoolItem

    val constantPoolGrammar = grammars.find(ConstantPoolGrammar)
    val entries: GrammarReference = new RootGrammar(constantPoolGrammar).find(p => p.get.isInstanceOf[ManyVertical]).
      get.asInstanceOf[GrammarReference] //TODO al die casts naar GrammarReference zijn loos. Beter altijd een GrammarReference returnen. O wacht, implicit cast naar grammarReference!!!
    entries.set(addIndicesToList(entries.get))
  }

  def addIndicesToList(listGrammar: BiGrammar): BiGrammar = {
    val removeIndexForParsing: (Any) => Seq[Any] = items => items.asInstanceOf[Seq[Node]].map(i => i(Content))
    val addIndexForPrinting: (Any) => Some[Seq[Node]] = items => Some(items.asInstanceOf[Seq[Any]].zipWithIndex.map(p => new Node(WithIndexClass,
      Index -> (p._2.asInstanceOf[Int] + 1),
      Content -> p._1)))
    listGrammar ^^ ( removeIndexForParsing, addIndexForPrinting )
  }

  override def description: String = "Add indices to the constant pool entries"
}