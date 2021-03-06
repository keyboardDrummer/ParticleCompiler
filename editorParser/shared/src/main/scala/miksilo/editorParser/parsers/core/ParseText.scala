package miksilo.editorParser.parsers.core

import miksilo.editorParser.LazyLogging
import miksilo.editorParser.parsers.editorParsers.{Position, SourceRange}

import scala.collection.Searching

// TODO Merge the position and the document state, so that the position in a zipper over the document state data structure.
final class ParseText extends CharSequence with LazyLogging {

  def this(text: String) = {
    this()
    arrayOfChars = text.toCharArray
  }

  private var _arrayOfChars: Array[Char] = Array.emptyCharArray
  private var lineStarts: Array[Int] = _

  def applyRangeChange(start: Int, end: Int, newText: String): Unit = {
    val newArray = new Array[Char](_arrayOfChars.length + newText.length - (end - start))
    _arrayOfChars.copyToArray(newArray, 0, start)
    Array.copy(_arrayOfChars, end, newArray, start + newText.length, _arrayOfChars.length - end)
    newText.copyToArray(newArray, start, newText.length)
    arrayOfChars = newArray
  }

  def arrayOfChars: Array[Char] = _arrayOfChars
  def arrayOfChars_=(arrayOfChars: Array[Char]): Unit = {
    _arrayOfChars = arrayOfChars
    lineStarts = {
      var offset = 0
      var result = List(0)
      var index = 0
      while(index < arrayOfChars.length) {
        val char = arrayOfChars(index)
        offset += 1
        if (char == '\n') {
          result ::= offset
        }
        index += 1
      }
      result.reverse.toArray
    }
  }

  def getOffset(position: Position): Int = {
    if (position.character == 0 && position.line == 0)
      return 0

    if (position.line < 0 || position.line >= lineStarts.length) {
      throw new IllegalArgumentException(s"Line ${position.line} is not in the document.")
    }
    lineStarts(position.line) + position.character
  }

  def getPosition(offset: Int): Position = {
    import Searching._
    lineStarts.search(offset) match {
      case Found(index) => Position(index, 0)
      case InsertionPoint(insertionPoint) =>
        val line = insertionPoint - 1
        Position(line, offset - lineStarts(line))
    }
  }

  def length: Int                                     = arrayOfChars.length
  def charAt(index: Int): Char                        = arrayOfChars(index)
  def subSequence(start: Int, end: Int): CharSequence = new runtime.ArrayCharSequence(arrayOfChars, start, end)
  override def toString                               = arrayOfChars.mkString
}
