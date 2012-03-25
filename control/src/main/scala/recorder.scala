package capture
package control

import java.io.{
  File,
  FileOutputStream
}

case class Frame(index: Long)
case object Quit

case class Record(path: String) extends actors.Actor {
  val location = new File(path)

  @volatile private var stopping = false

  def writeImage(index: Long, data: Array[Byte]) = {
    val name = "%05d.jpg" format index
    val writer = new FileOutputStream(new File(location, name))
    writer.write(data)
    writer.close()
  }

  def isRunning() = !stopping

  def act = {
    stopping = false
    if (!location.exists) {
      location.mkdirs()
    }

    this ! Frame(0L)
    loopWhile(isRunning()) {
      react {
        case Frame(index) =>
          writeImage(index, Robot.screenshot.withPointer.data(1.0f))
          Thread.sleep(1000 / 50)
          this ! Frame(index + 1)
        case Quit => stopping = true
      }
    }
  }

  def stop() = this ! Quit
}
