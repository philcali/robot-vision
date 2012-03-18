package capture
package server

import control.Robot

case class ImageProps(scaleX: Double, scaleY: Double, qual: Float, p: Boolean)
case object Write
case object Quit

// Start the image service serving at the specified frame rate
object ImageService extends actors.Actor { service =>
  @volatile private var stopping = false

  @volatile private var settings = ImageProps(1.0, 1.0, 0.2f, true)

  def act = {
    loopWhile(isRunning) {
      react {
        case ImageProps(x, y, q, p) => settings = ImageProps(x, y, q, p)
        case Quit => stopping = true
        case Write => forwardWrite(); sender ! Write
      }
    }
  }

  def forwardWrite() {
    val ImageProps(x, y, q, p) = settings
    val shot = if (p) Robot.screenshot.withPointer else Robot.screenshot
    val data = if (x == 1.0 && x == y)
      shot.data(q) else shot.scale(x, y).data(q)
    ImageStream.write(data)
  }

  def stop() = service ! Quit

  def isRunning() = !stopping
}

case class Pulse(rate: Int) extends actors.Actor { 
  def act = {
    this ! Write
    loopWhile(ImageService.isRunning()) {
      react {
        case Write =>
          Thread.sleep(rate)
          ImageService ! Write
      }
    }
  }
}
