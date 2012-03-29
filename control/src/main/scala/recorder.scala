package capture
package control

import java.io.{
  File,
  FileOutputStream
}

case class Frame(index: Long)
case object Quit

case class Record(path: String) extends actors.Actor { self =>
  val location = new File(path)

  @volatile private var stopping = true 

  def writeImage(index: Long)(out: FileOutputStream => Unit) {
    val name = "%05d.jpg" format index
    val writer = new FileOutputStream(new File(location, name))
    out(writer)
  }

  def isRunning() = !stopping

  def act = {
    stopping = false

    if (!location.exists) {
      location.mkdirs()
    }

    self ! Frame(1)
    loopWhile(isRunning()) {
      react {
        case Frame(index) =>
          writeImage(index)(Robot.screenshot.withPointer.output(1.0f, _))
          Thread.sleep(1000 / 50)
          self ! Frame(index + 1)
        case Quit =>
          stopping = true
      }
    }
  }

  def stop() = this ! Quit
}
