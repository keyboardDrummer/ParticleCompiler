package transformations.javac.classes

import core.transformation.grammars.GrammarCatalogue
import core.transformation.sillyCodePieces.GrammarTransformation
import core.transformation.{Contract, MetaObject, TransformationState}

object BasicImportC extends GrammarTransformation {

  object ImportKey
  object ElementsKey

  object ImportPathGrammar

  def _import(elements: Seq[String]) = new MetaObject(ImportKey, ElementsKey -> elements)

  override def transformGrammars(grammars: GrammarCatalogue): Unit = {
    val importPath = grammars.create(ImportPathGrammar, identifier.someSeparated(".") ^^ parseMap(ImportKey, ElementsKey))
    val basicImport = "import" ~~> importPath <~ ";"
    grammars.find(ClassC.ImportGrammar).addOption(basicImport)
  }

  def getParts(_import: MetaObject) = _import(ElementsKey).asInstanceOf[Seq[String]]

  override def inject(state: TransformationState): Unit = {
    ClassC.getState(state).importToClassMap.put(ImportKey, _import => {
      val elements = getParts(_import)
      val packageParts = elements.dropRight(1)
      val importedClassName = elements.last

      val qualifiedClassName = new QualifiedClassName(packageParts ++ Seq(importedClassName))
      val result = Seq((importedClassName, qualifiedClassName)).toMap
      result
    })
    super.inject(state)
  }

  override def dependencies: Set[Contract] = Set(ClassC)
}
