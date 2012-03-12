package capture
package server

import control.Robot

import unfiltered.request._
import unfiltered.response._

import unfiltered.netty.cycle.Planify

object DesktopImage {
  val Pattern = """desktop_(\d+\.\d+)x(\d+\.\d+)_(\d\.\d+)_([p|n])\.jpg""".r

  def unapply(image: String) = image match {
    case Pattern(x, y, q, p) =>
      Some((x.toDouble, y.toDouble, q.toFloat, p == "p"))
    case _ => Some((1.0, 1.0, 0.2f, false))
  }
}

object Vision extends DefaultPlan with Lmxml with ResourceLoader {
  import lmxml.transforms.Empty

  val validFiles = List("jquery.js", "desktop.js", "interface.js", "viewport.js")

  def data = Seq("connect-check" -> Empty)

  def intent = {
    case Path("/view.html") =>
      index(retrieve("index.lmxml"))
    case Path(Seg(ValidJs(file) :: Nil)) =>
      Ok ~> ContentType("text/javascript") ~> ResponseString(retrieve(file))
    case Path(Seg("image" :: DesktopImage(x, y, quality, pointer) :: Nil)) =>
      val screenshot = if (pointer)
        Robot.screenshot.withPointer
      else Robot.screenshot

      val bytes = if (x == 1.0 && x == y)
        screenshot.data(quality) else screenshot.scale(x, y).data(quality)

      Ok ~> ContentType("image/jpeg") ~> ResponseBytes(bytes)
  }
}
