package capture
package server

import unfiltered.request._
import unfiltered.response._

import unfiltered.netty._

object Connect extends DefaultPlan with Lmxml {
  import lmxml.transforms.If

  val validFiles = List("connect.js", "control.js")

  def data = Seq("connect-check" -> If (true)(Nil))

  def intent = {
    case req @ Path("/desktop.html") =>
      req.respond(index(Resource.retrieve("index.lmxml")))
    case req @ Path(Seg(ValidJs(rf) :: Nil)) => req.respond(rf)
  }
}
