package core.transformation

import core.grammar._

class GrammarCatalogue {
  var grammars: Map[Any, Labelled] = Map.empty

  def find(key: Any): Labelled = grammars(key)

  def create(key: AnyRef, inner: Grammar = null): Labelled = {
    val result = new Labelled(key, inner)
    grammars += key -> result
    result
  }

  def getGrammars: Set[Grammar] = {

    var closed = Set.empty[Grammar]
    def inner(grammar: Grammar): Unit = {

      if (closed.contains(grammar))
        return

      closed += grammar
      grammar match {
        case labelled: Labelled => inner(labelled.inner)
        case sequence: Sequence =>
          inner(sequence.first)
          inner(sequence.second)
        case choice: Choice =>
          inner(choice.left)
          inner(choice.right)
        case map: MapGrammar => inner(map.inner)
        case many: Many => inner(many.inner)
        case _ => Set.empty
      }
    }

    grammars.values.foreach(labelled => inner(labelled))
    closed
  }
}

trait GrammarTransformation extends Injector with GrammarWriter {
  def transformGrammars(grammars: GrammarCatalogue)
}

trait Injector extends Contract {
  def inject(state: TransformationState) = {}
}

trait ProgramTransformation extends Injector {
  def transform(program: MetaObject, state: TransformationState)
}
