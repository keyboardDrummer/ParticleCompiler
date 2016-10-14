package transformations.javac.statements

import core.particles.grammars.GrammarCatalogue
import core.particles.node.{Key, Node}
import core.particles.path.Path
import core.particles.{CompilationState, ParticleWithGrammar}
import transformations.bytecode.additions.LabelledLocations
import transformations.bytecode.simpleBytecode.InferredStackFrames



object ContinueC extends StatementInstance {
  override val key: Key = ContinueKey

  object ContinueKey extends Key
  def continue = new Node(ContinueKey)

  override def toByteCode(statement: Path, state: CompilationState): Seq[Node] = {
    val startLabel = WhileC.getState(state).whileStartLabels(getWhileParent(statement))
    Seq(LabelledLocations.goTo(startLabel))
  }

  override def transformGrammars(grammars: GrammarCatalogue): Unit = {
    val statementGrammar = grammars.find(StatementSkeleton.StatementGrammar)
    statementGrammar.addOption("continue;" ~> produce(continue))
  }

  override def description: String = "Jumps the program to the start of the loop."

  override def getNextStatements(obj: Path, labels: Map[Any, Path]): Set[Path] = {
    val _whileParent: Path = getWhileParent(obj)
    Set(labels(WhileC.startKey(_whileParent.current)))
  }

  def getWhileParent(obj: Path): Path = {
    val ancestors = obj.ancestors
    val _whileParent = ancestors.filter(ancestor => ancestor.clazz == WhileC.WhileKey).head
    _whileParent
  }
}
