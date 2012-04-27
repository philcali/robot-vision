package capture
package server

import control.Robot

import unfiltered.request._
import unfiltered.response._

import unfiltered.netty.async

object Vision extends Interface {
  def enableControl = true

  def preload = {
    case req @ Path(Seg("image" :: DesktopImage(x, y, q, p) :: Nil)) =>
      val screenshot = if (p) Robot.screenshot.withPointer
        else Robot.screenshot

      val bytes = if (x == 1.0 && x == y)
        screenshot.data(q) else screenshot.scale(x, y).data(q)

      req.respond(
        Ok ~> ContentType("image/jpeg") ~> ResponseBytes(bytes)
      )
  }
}
