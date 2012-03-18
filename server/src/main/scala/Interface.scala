package capture
package server

import unfiltered.request._
import unfiltered.response._

import unfiltered.netty.async
import unfiltered.netty.ServerErrorResponse

trait Interface extends DefaultPlan with Lmxml {
  import lmxml.transforms.Empty

  val validFiles =
    List("jquery.js", "desktop.js", "interface.js", "viewport.js")

  def data = Seq("connect-check" -> Empty)

  def preload: async.Plan.Intent

  def defaults: async.Plan.Intent = {
    case req @ Path("/view.html") =>
      req.respond(index(Resource.retrieve("index.lmxml")))
    case req @ Path(Seg(ValidJs(rf) :: Nil)) => req.respond(rf)
    case req @ Path(Seg(Bootstrap(rf) :: Nil)) => req.respond(rf)
    case req @ Path(Seg("img" :: Bootstrap(rf) :: Nil)) => req.respond(rf)
  }

  def intent = preload orElse defaults
}

trait DefaultPlan extends async.Plan with ServerErrorResponse {
  // Allow plan to determine what js files are valid
  val validFiles: List[String]

  object ValidJs {
    def unapply(file: String) = {
      if (validFiles.exists(_ == file))
        Some(
          Ok ~>
          ContentType("text/javascript") ~>
          ResponseString(Resource.retrieve(file))
        )
      else
        None
    }
  }
}

object Bootstrap {
  object Js {
    val Pattern = """^bootstrap\-\w+\.js""".r

    def unapply(script: String) =
      Pattern.findFirstIn(script).map("js/" + _)
  }

  object Glyphs {
    val Pattern = """.*\.png$""".r

    def unapply(path: String) =
      Pattern.findFirstIn(path).map("img/" + _)
  }

  def respond(t: String, contents: Array[Byte]) = {
    Ok ~> ContentType(t) ~> ResponseBytes(contents)
  }

  def unapply(file: String) = file match {
    case "bootstrap.css" =>
      Some(respond("text/css",
        Resource.retrieveBytes("css/less/bootstrap.css")))
    case Js(Resource(bytes)) =>
      Some(respond("text/javascript", bytes))
    case Glyphs(Resource(image)) =>
      Some(respond("image/png", image))
    case _ => None
  }
}
