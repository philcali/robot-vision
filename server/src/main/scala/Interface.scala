package capture
package server

import unfiltered.request._
import unfiltered.response._

import unfiltered.netty.async
import unfiltered.netty.ServerErrorResponse

case class Attachment(filename: String) extends Responder[Any] {
  def respond(res: HttpResponse[Any]) {
    res.header("Content-Disposition", "attachment; filename=%s".format(filename))
  }
}

trait Interface extends DefaultPlan with Lmxml {
  import lmxml.transforms.{ Empty, If }

  val pattern = new java.text.SimpleDateFormat("MM-dd-yy KK:mm:ss a")

  val validFiles =
    List("lib/jquery.js", "lib/desktop.js", "interface.js", "lib/viewport.js")

  def data = Seq(
    "connect-check" -> Empty, "enable-control" -> If (enableControl)(Nil)
  )

  def enableControl: Boolean

  def preload: async.Plan.Intent

  def defaults: async.Plan.Intent = {
    case req @ Path("/robot-vision.html") =>
      req.respond(index(Resource.retrieve("index.lmxml")))
    case req @ Path(StripSlash(ValidJs(rf))) => req.respond(rf)
    case req @ Path(Seg(Bootstrap(rf) :: Nil)) => req.respond(rf)
    case req @ Path(Seg("img" :: Bootstrap(rf) :: Nil)) => req.respond(rf)
    case req @ Path(Seg("snapshot.jpg" :: Nil)) =>
      val now = new java.util.Date(System.currentTimeMillis)
      req.respond(
        Ok ~>
        Attachment("Snapshot-%s.jpg".format(pattern.format(now))) ~>
        ContentType("image/jpeg") ~>
        ResponseBytes(capture.control.Robot.screenshot.withPointer.data(1.0f))
      )
  }

  def intent = preload orElse defaults
}

trait DefaultPlan extends async.Plan with ServerErrorResponse {
  // Allow plan to determine what js files are valid
  val validFiles: List[String]

  object StripSlash {
    def unapply(path: String) =
      if (path.startsWith("/")) Some(path.drop(1).mkString) else None
  }

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
