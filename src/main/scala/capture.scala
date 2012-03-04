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
import java.awt.event.{
  InputEvent,
  KeyEvent
}
import java.awt.image.RenderedImage
import javax.imageio.{
  ImageIO,
  ImageWriteParam,
  IIOImage
}
import javax.imageio.stream.MemoryCacheImageOutputStream
import java.io.ByteArrayOutputStream

object Remote {
  private val robot = new Robot()

  def apply[A](fun: Robot => A): Option[A] = allCatch opt(fun(robot))
}

object Screenshot {
  def screen = allCatch opt {
    val src = Toolkit.getDefaultToolkit.getScreenSize
    new Rectangle(src)
  } getOrElse (new Rectangle(800, 600))

  def take = Remote(_.createScreenCapture(screen))

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

class EventParsing(val ident: String) {
  def unapplySeq(text: String) = {
    if (!text.startsWith(ident)) None else
      Some(text.split("\\|").drop(1).toSeq)
  }
}

trait MouseTranslator {
  def translate(button: String) = button match {
    case "0" => InputEvent.BUTTON1_MASK
    case "2" => InputEvent.BUTTON3_MASK
    case "1" => InputEvent.BUTTON2_MASK
  }
}

trait KeyTranslator {
  def translate(key: String) = key match {
    case "65" => KeyEvent.VK_A
    case "66" => KeyEvent.VK_B
    case "67" => KeyEvent.VK_C
    case "68" => KeyEvent.VK_D
    case "69" => KeyEvent.VK_E
    case "70" => KeyEvent.VK_F
    case "71" => KeyEvent.VK_G
    case "72" => KeyEvent.VK_H
    case "73" => KeyEvent.VK_I
    case "74" => KeyEvent.VK_J
    case "75" => KeyEvent.VK_K
    case "76" => KeyEvent.VK_L
    case "77" => KeyEvent.VK_M
    case "78" => KeyEvent.VK_N
    case "79" => KeyEvent.VK_O
    case "80" => KeyEvent.VK_P
    case "81" => KeyEvent.VK_Q
    case "82" => KeyEvent.VK_R
    case "83" => KeyEvent.VK_S
    case "84" => KeyEvent.VK_T
    case "85" => KeyEvent.VK_U
    case "86" => KeyEvent.VK_V
    case "87" => KeyEvent.VK_W
    case "88" => KeyEvent.VK_X
    case "89" => KeyEvent.VK_Y
    case "90" => KeyEvent.VK_Z
    case "13" => KeyEvent.VK_ENTER
    case "8" => KeyEvent.VK_BACK_SPACE
    case "9" => KeyEvent.VK_TAB
    case "32" => KeyEvent.VK_SPACE
    case "16" => KeyEvent.VK_SHIFT
    case "17" => KeyEvent.VK_CONTROL
    case "18" => KeyEvent.VK_ALT
    case "37" => KeyEvent.VK_LEFT
    case "38" => KeyEvent.VK_UP
    case "39" => KeyEvent.VK_RIGHT
    case "40" => KeyEvent.VK_DOWN
    case "48" => KeyEvent.VK_0
    case "49" => KeyEvent.VK_1
    case "50" => KeyEvent.VK_2
    case "51" => KeyEvent.VK_3
    case "52" => KeyEvent.VK_4
    case "53" => KeyEvent.VK_5
    case "54" => KeyEvent.VK_6
    case "55" => KeyEvent.VK_7
    case "56" => KeyEvent.VK_8
    case "57" => KeyEvent.VK_9
    case _ => KeyEvent.CHAR_UNDEFINED
  }
}

object DesktopRender extends EventParsing("render")

object MouseDown extends EventParsing("mousedown") with MouseTranslator
object MouseUp extends EventParsing("mouseup") with MouseTranslator
object MouseMove extends EventParsing("mousemove")

object KeyUp extends EventParsing("keyup") with KeyTranslator
object KeyDown extends EventParsing("keydown") with KeyTranslator

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
      "width" -> Value(screen.getWidth.toInt),
      "height" -> Value(screen.getHeight.toInt)
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
        case KeyUp(keycode) =>
          Remote(_.keyRelease(KeyUp.translate(keycode)))
        case KeyDown(keycode) =>
          Remote(_.keyPress(KeyDown.translate(keycode)))
        case MouseUp(button) =>
          Remote(_.mouseRelease(MouseUp.translate(button)))
        case MouseDown(button) =>
          Remote(_.mousePress(MouseDown.translate(button)))
        case MouseMove(xStr, yStr) =>
          Remote(_.mouseMove(xStr.toInt, yStr.toInt))
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
