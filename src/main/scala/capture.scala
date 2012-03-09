package capture

import control._

// Scala Imports
import lmxml.{
  LmxmlConvert,
  Conversion,
  PlainLmxmlParser
}
import lmxml.shortcuts.html.HtmlShortcuts

import unfiltered.request._
import unfiltered.response._
import unfiltered.filter.request.ContextPath

import unfiltered.netty.Http
import unfiltered.netty.websockets._

object Lmxml extends Conversion {
  def createParser(step: Int) = new PlainLmxmlParser(step) with HtmlShortcuts
}

object Main extends App {
  import lmxml.XmlConvert
  import lmxml.transforms._

  def pump(in: java.io.InputStream, out: java.io.OutputStream): Unit = {
    val bytes = new Array[Byte](1024)
    in.read(bytes) match {
      case n if n > 0 => out.write(bytes, 0, n); pump(in, out)
      case _ => in.close(); out.close()
    }
  }

  def index(source: String) = {
    val screen = Robot.display

    val trans = Transform(
      "width" -> Value(screen.getWidth.toInt),
      "height" -> Value(screen.getHeight.toInt)
    )

    val response = Lmxml.convert(source)(trans andThen XmlConvert).toString
    Ok ~> HtmlContent ~> ResponseString(response)
  }

  def retrieve(file: String) = {
    val s = getClass.getResourceAsStream("/" + file)
    val bs = new java.io.ByteArrayOutputStream()
    pump(s, bs)

    new String(bs.toByteArray, "UTF-8")
  }

  Http(8080).handler(Planify({
    case _ => {
      case Message(s, Text(msg)) => msg match {
        case KeyUp(keycode) =>
          Robot(_.keyRelease(KeyTranslate(keycode)))
        case KeyDown(keycode) =>
          Robot(_.keyPress(KeyTranslate(keycode)))
        case MouseUp(button) =>
          Robot(_.mouseRelease(MouseTranslate(button)))
        case MouseDown(button) =>
          Robot(_.mousePress(MouseTranslate(button)))
        case MouseMove(xStr, yStr) =>
          Robot(_.mouseMove(xStr.toInt, yStr.toInt))
      }
    }
  }).onPass(_.sendUpstream(_))).handler(unfiltered.netty.cycle.Planify {
    case Path("/desktop.html") =>
      index(retrieve("index.lmxml"))
    case Path("/connect.js") =>
      Ok ~> ContentType("text/javascript") ~> ResponseString(retrieve("connect.js"))
    case Path(Seg("image" :: "desktop.jpg" :: Nil)) =>
      Ok ~> ContentType("image/jpeg") ~> ResponseBytes(Robot.screenshot.data())
  }).run()
}
