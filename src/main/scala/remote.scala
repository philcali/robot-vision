package capture
package control

// Scala
import util.control.Exception.allCatch

// Java
import java.io.ByteArrayOutputStream
import java.awt.{
  Toolkit,
  Rectangle
}

import java.awt.image.BufferedImage
import javax.imageio.{
  ImageIO,
  ImageWriteParam,
  IIOImage
}

import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import javax.imageio.stream.MemoryCacheImageOutputStream

object Robot {
  import java.awt.{ Robot => JBot }

  private val robot = new JBot()

  def apply[A](fun: JBot => A): Option[A] = allCatch opt(fun(robot))

  def display = allCatch opt {
    val src = Toolkit.getDefaultToolkit.getScreenSize
    new Rectangle(src)
  } getOrElse (new Rectangle(800, 600))

  def screenshot = Screenshot(this(_.createScreenCapture(display)).get)
}

case class Screenshot(image: BufferedImage) {
  def prep = {
    new BufferedImage(image.getWidth, image.getHeight, image.getType)
  }

  def scale(byX: Double, byY: Double) = {
    val at = new AffineTransform()
    at.scale(byX, byY)
    
    val result = prep
    
    val scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR)
    scaleOp.filter(image, result)

    Screenshot(result)
  }

  def data(quality: Float = 0.2f) = {
    val buffer = new ByteArrayOutputStream()
    val cache = new MemoryCacheImageOutputStream(buffer)

    val writer = ImageIO.getImageWritersByFormatName("jpeg").next
    val param = writer.getDefaultWriteParam()

    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
    param.setCompressionQuality(quality)

    writer.setOutput(cache)
    writer.write(null, new IIOImage(image, null, null), param)
    writer.dispose()

    buffer.toByteArray
  }
}
