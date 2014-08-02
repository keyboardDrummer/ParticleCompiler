package transformations.javac

import org.junit.{Assert, Test}

import scala.reflect.io.Path

class TestObjectTypeUnification {

  @Test
  def testFullPipeline() {
    val inputDirectory = Path("remy")
    val output: String = TestUtils.compileAndRun("ObjectTypeUnification", inputDirectory)
    Assert.assertEquals("Object", output)
  }
}
