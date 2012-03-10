package capture
package server

import control.Robot

import unfiltered.request._
import unfiltered.response._

import unfiltered.netty.cycle.Planify

object DesktopImage {
  val Pattern = """desktop_(\d+\.\d+)x(\d+\.\d+)_(\d\.\d+)\.jpg""".r

  def unapply(image: String) = image match {
    case Pattern(x, y, q) => Some((x.toDouble, y.toDouble, q.toFloat))
    case _ => Some((1.0, 1.0, 0.2f))
  }
}

object Vision extends Planify({
  case Path(Seg("image" :: DesktopImage(x, y, quality) :: Nil)) =>
    val screenshot = Robot.screenshot

    val bytes = if (x == 1.0 && x == y)
      screenshot.data(quality) else screenshot.scale(x, y).data(quality)

    Ok ~> ContentType("image/jpeg") ~> ResponseBytes(bytes)
})
