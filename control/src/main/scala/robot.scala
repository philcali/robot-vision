package capture
package control

// Scala
import util.control.Exception.allCatch

// Java
import java.io.ByteArrayOutputStream

import java.awt.{
  Color,
  Toolkit,
  MouseInfo,
  Rectangle,
  Robot => JBot,
  GraphicsEnvironment => GE
}

import java.awt.datatransfer.{
  DataFlavor,
  StringSelection
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

object Clipboard {
  lazy val system = Toolkit.getDefaultToolkit.getSystemClipboard

  def apply(text: String) = allCatch opt {
    system.setContents(new StringSelection(text), null)
  }

  def retrieve =
    allCatch opt (system.getContents(None)) filter(_ != null) map { contents =>
      contents.getTransferData(DataFlavor.stringFlavor).toString
    }
}

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
  val pointer = ImageIO.read(getClass.getResourceAsStream("/pointer.png"))

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
