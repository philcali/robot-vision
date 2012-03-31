package capture
package control

package test

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class RobotTest extends FlatSpec with ShouldMatchers {
  trait Reporter extends Record {
    override def handleIndex(index: Long) = {
      println(index)
      super.handleIndex(index)
    }
  }

  def time(work: => Unit) = {
    val start = System.currentTimeMillis.toDouble
    work
    System.currentTimeMillis.toDouble - start
  }

  "Robot" should "hit the desired framerate" in {
    val folderName = "record-test"
    val folder = new java.io.File(folderName)

    val recorder = new Record(folderName) with Reporter

    recorder.start()
    Thread.sleep(1000 * 60)
    recorder.stop()
    println("TIME!")

    folder.listFiles.foreach(_.delete)
    folder.delete
  }

  // Worker id system millis
  // Super controls worker threads
  // Long running contains a super to listen to shutdowns
}
