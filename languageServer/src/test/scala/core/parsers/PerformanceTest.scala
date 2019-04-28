package core.parsers

import deltas.json.JsonLanguage
import org.scalatest.FunSuite
import util.SourceUtils

class PerformanceTest extends FunSuite {

  test("correct JSON performance") {
    val source = SourceUtils.getTestFileContents("AutoScalingMultiAZWithNotifications.json")
    val json = JsonLanguage.language
    val multiplier = 1
    val tenTimesSource = s"[${1.to(10).map(_ => source).reduce((a,b) => a + "," + b)}]"

    val timeA = System.currentTimeMillis()
    for(_ <- 1.to(multiplier * 10)) {
      json.compileString(source)
    }

    val timeB = System.currentTimeMillis()
    for(_ <- 1.to(multiplier)) {
      json.compileString(tenTimesSource)
    }

    val timeC = System.currentTimeMillis()

    val singleSource = timeB - timeA
    val sourceTimesTen = timeC - timeB
    assert(singleSource < 4000 * multiplier)
    System.out.println(s"singleSource:$singleSource")
    System.out.println(s"totalTime:${singleSource + sourceTimesTen}")
  }
}
