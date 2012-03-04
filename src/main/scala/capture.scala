package capture

// Scala Imports
import lmxml.{
  LmxmlConvert,
  Conversion,
  PlainLmxmlParser
}
import lmxml.shortcuts.html.HtmlShortcuts

import util.control.Exception.allCatch

import unfiltered.request._
import unfiltered.response._
import unfiltered.filter.request.ContextPath

import unfiltered.netty.Http
import unfiltered.netty.websockets._

// Java Imports
import java.awt.{
  Toolkit,
  Rectangle,
  Robot
}
import java.awt.image.RenderedImage
import javax.imageio.{
  ImageIO,
  ImageWriteParam,
  IIOImage
}
import javax.imageio.stream.MemoryCacheImageOutputStream
import java.io.ByteArrayOutputStream

object Screenshot {
  def screen = allCatch opt {
    val src = Toolkit.getDefaultToolkit.getScreenSize
    new Rectangle(src)
  } getOrElse (new Rectangle(800, 600))

  def take = {
    allCatch opt (new Robot().createScreenCapture(screen))
  }

  def data = take.map(toData).getOrElse(new Array[Byte](0))

  def toData(img: RenderedImage) = {
    val buffer = new ByteArrayOutputStream()
    val cache = new MemoryCacheImageOutputStream(buffer)

    val writer = ImageIO.getImageWritersByFormatName("jpeg").next
    val param = writer.getDefaultWriteParam()

    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
    param.setCompressionQuality(0.3f)

    writer.setOutput(cache)
    writer.write(null, new IIOImage(img, null, null), param)
    writer.dispose()

    buffer.toByteArray
  }
}

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
    val screen = Screenshot.screen

    val trans = Transform(
      "width" -> Value(screen.getWidth),
      "height" -> Value(screen.getHeight)
    )

    val response = Lmxml.convert(source)(trans andThen XmlConvert).toString
    Ok ~> HtmlContent ~> ResponseString(response)
  }

  def retrieve(file: String) = {
    val s = getClass.getResourceAsStream("/" + file)
    val bs = new ByteArrayOutputStream()
    pump(s, bs)

    new String(bs.toByteArray, "UTF-8")
  }

  Http(8080).handler(Planify({
    case _ => {
      case Message(s, Text(msg)) => msg match {
        case "render" => s.send("reload")
        case "keypress" =>
        case "mousemove" =>
      }
    }
  }).onPass(_.sendUpstream(_))).handler(unfiltered.netty.cycle.Planify {
    case Path("/desktop.html") =>
      index(retrieve("index.lmxml"))
    case Path("/connect.js") =>
      Ok ~> ContentType("text/javascript") ~> ResponseString(retrieve("connect.js"))
    case Path(Seg("image" :: "desktop.jpg" :: Nil)) =>
      Ok ~> ContentType("image/jpeg") ~> ResponseBytes(Screenshot.data)
  }).run()
}
