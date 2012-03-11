package capture
package server

import lmxml.{
  LmxmlConvert,
  Conversion,
  PlainLmxmlParser
}

import unfiltered.request._
import unfiltered.response._

import lmxml.shortcuts.html.HtmlShortcuts

import unfiltered.netty.ServerErrorResponse
import unfiltered.netty.cycle._

trait ResourceLoader {
  def pump(in: java.io.InputStream, out: java.io.OutputStream): Unit = {
    val bytes = new Array[Byte](1024)
    in.read(bytes) match {
      case n if n > 0 => out.write(bytes, 0, n); pump(in, out)
      case _ => in.close(); out.close()
    }
  }

  def retrieve(file: String) = {
    val s = getClass.getResourceAsStream("/" + file)
    val bs = new java.io.ByteArrayOutputStream()
    pump(s, bs)

    new String(bs.toByteArray, "UTF-8")
  }
}

trait DefaultPlan extends Plan with ThreadPool with ServerErrorResponse

trait Lmxml extends Conversion {
  import control.Robot
  import lmxml.XmlConvert
  import lmxml.transforms._

  def createParser(step: Int) = new PlainLmxmlParser(step) with HtmlShortcuts

  def screenData = Seq(
    "width" -> Value(Robot.display.getWidth.toInt),
    "height" -> Value(Robot.display.getHeight.toInt)
  )

  def data: Seq[(String, Processor)]

  def index(source: String) = {
    val trans = Transform((screenData ++ data):_*)

    Ok ~> Html(convert(source)(trans andThen XmlConvert))
  }
}

object Connect extends DefaultPlan with Lmxml with ResourceLoader {
  import lmxml.transforms.If

  def data = Seq("connect-check" -> If (true)(Nil))

  def intent = {
    case Path("/desktop.html") =>
      index(retrieve("index.lmxml"))
    case Path("/connect.js") =>
      Ok ~> ContentType("text/javascript") ~> ResponseString(retrieve("connect.js"))
  }
}
