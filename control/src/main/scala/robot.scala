package capture
package control

// Java
import java.io.ByteArrayOutputStream

import java.awt.{
  Color,
  MouseInfo,
  Rectangle,
  Robot => JBot,
  GraphicsEnvironment => GE
}

import java.awt.event.{
  InputEvent,
  KeyEvent
}

import java.awt.image.{
  BufferedImage,
  ImageObserver
}

import javax.imageio.{
  ImageIO,
  ImageWriteParam,
  IIOImage
}

import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import javax.imageio.stream.MemoryCacheImageOutputStream


import util.control.Exception.allCatch

// TODO: rethink possibility of having independent coordinate systems
object Robot {
  private val robot = new JBot()

  private val screens = GE.getLocalGraphicsEnvironment.getScreenDevices()

  lazy val display = allCatch opt {
    screens.foldLeft(new Rectangle) { (in, screen) => 
      val bounds = screen.getDefaultConfiguration.getBounds()
      new Rectangle(in.width + bounds.width, math.max(in.height, bounds.height))
    }
  } getOrElse (new Rectangle(800, 600))

  def apply[A](fun: JBot => A): Option[A] = allCatch opt(fun(robot))

  def screenshot = Screenshot(robot.createScreenCapture(display))

  def mouse = allCatch opt(MouseInfo.getPointerInfo) map { info =>
    (info.getLocation.x, info.getLocation.y)
  } getOrElse((0, 0))

  def clearInputs() {
    (1 to 222).map(k => apply(_.keyRelease(KeyTranslate(k.toString))))
    (0 to 2).map(i => apply(_.mouseRelease(MouseTranslate(i.toString))))
  }
}

object Screenshot {
  lazy val pointer = ImageIO.read(getClass.getResourceAsStream("/pointer.png"))

  def apply(image: BufferedImage) = new Screenshot(image)
}

class Screenshot(image: BufferedImage) {
  def prep(x: Double, y: Double) = {
    new BufferedImage(
      (image.getWidth.toDouble * x).toInt,
      (image.getHeight.toDouble * y).toInt,
      image.getType
    )
  }

  def withPointer = {
    val result = new BufferedImage(
      image.getColorModel, image.getRaster, true, null
    )

    val (x, y) = Robot.mouse
    val g = result.getGraphics

    g.drawImage(Screenshot.pointer, x - 6, y - 4, null)

    Screenshot(result)
  }

  def scale(byX: Double, byY: Double) = {
    val at = new AffineTransform()
    at.scale(byX, byY)
    
    val result = prep(byX, byY)
    
    val scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR)
    scaleOp.filter(image, result)

    Screenshot(result)
  }

  def data(quality: Float = 0.2f) = {
    val buffer = new ByteArrayOutputStream()

    output(quality, buffer)

    buffer.toByteArray
  }

  def output(quality: Float = 2.0f, out: java.io.OutputStream) {
    val cache = new MemoryCacheImageOutputStream(out)

    val writer = ImageIO.getImageWritersByFormatName("jpeg").next
    val param = writer.getDefaultWriteParam()

    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
    param.setCompressionQuality(quality)

    writer.setOutput(cache)
    writer.write(null, new IIOImage(image, null, null), param)
    writer.dispose()

    cache.close()
  }
}

object MouseTranslate {
  def unapply(button: String) = allCatch opt(apply(button))

  def apply(button: String) = button match {
    case "0" => InputEvent.BUTTON1_MASK
    case "2" => InputEvent.BUTTON3_MASK
    case "1" => InputEvent.BUTTON2_MASK
  }
}

object KeyTranslate {
  def unapply(key: String) = allCatch opt(apply(key))

  def apply(key: String) = key match {
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
    case "191" => KeyEvent.VK_SLASH
    case "220" => KeyEvent.VK_BACK_SLASH
    case "219" => KeyEvent.VK_OPEN_BRACKET
    case "221" => KeyEvent.VK_CLOSE_BRACKET
    case "190" => KeyEvent.VK_PERIOD
    case "187" => KeyEvent.VK_EQUALS
    case "189" => KeyEvent.VK_MINUS
    case "192" => KeyEvent.VK_BACK_QUOTE
    case "222" => KeyEvent.VK_QUOTE
    case "188" => KeyEvent.VK_COMMA
    case "186" => KeyEvent.VK_SEMICOLON
    case _ => KeyEvent.CHAR_UNDEFINED
  }
}
