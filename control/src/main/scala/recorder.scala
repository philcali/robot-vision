package capture
package control

import java.io.{
  File,
  FileOutputStream
}

case class Frame(index: Long)
case object Quit

trait IncrementalRunning extends LongRunning {
  def nextIndex(index: Long) = index + 1
}

trait ForeverRunning extends LongRunning {
  def nextIndex(index: Long) = index
}

trait LongRunning extends actors.Actor { self =>
  val delay: Long

  @volatile protected var stopping = true
 
  def nextIndex(currentIndex: Long): Long
 
  def handleIndex(index: Long)

  def isRunning() = !stopping

  def responder: PartialFunction[Any,Unit] = {
    case Frame(index) =>
      handleIndex(index)
      Thread.sleep(delay)
      self ! Frame(nextIndex(index))
    case Quit =>
      stopping = true
  }

  def act = {
    stopping = false

    self ! Frame(1)
    loopWhile(isRunning()) (react(responder))
  }

  def stop() {
    self ! Quit
  }
}

case class Record(path: String) extends IncrementalRunning {
  val delay = 1000L / 20L

  val location = new File(path)

  def handleIndex(index: Long) {
    if (!location.exists) {
      location.mkdirs
    }

    val name = "%07d.jpg" format index
    val writer = new FileOutputStream(new File(location, name))
    Robot.screenshot.withPointer.output(1.0f, writer)
  }
}
