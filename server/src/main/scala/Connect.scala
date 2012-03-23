package capture
package server

import unfiltered.request._
import unfiltered.response._

import unfiltered.netty._

case class Connect(secret: String) extends DefaultPlan with Lmxml {
  import lmxml.transforms.{ If, Value }

  val validFiles = List("lib/connect.js", "lib/control.js")

  def data = Seq(
    "connect-check" -> If (true)(Nil),
    "robot-key" -> Value(secret)
  )

  def intent = {
    case req @ Path("/desktop.html") =>
      req.respond(index(Resource.retrieve("index.lmxml")))
    case req @ Path(StripSlash(ValidJs(rf))) => req.respond(rf)
  }
}
