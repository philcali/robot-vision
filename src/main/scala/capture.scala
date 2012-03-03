package capture

// Scala Imports
import util.control.Exception.allCatch

import unfiltered.request._
import unfiltered.response._
import unfiltered.jetty.Http

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
  def take = {
    allCatch opt {
      val scr = Toolkit.getDefaultToolkit.getScreenSize
      new Robot().createScreenCapture(new Rectangle(scr))
    }
  }
}

object DesktopImage extends unfiltered.filter.Plan {
  def stream(img: RenderedImage) = {
    val buffer = new ByteArrayOutputStream()
    val cache = new MemoryCacheImageOutputStream(buffer)

    val writer = ImageIO.getImageWritersByFormatName("jpeg").next
    val param = writer.getDefaultWriteParam()

    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
    param.setCompressionQuality(0.3f)

    writer.setOutput(cache)
    writer.write(null, new IIOImage(img, null, null), param)
    writer.dispose()

    Status(200) ~>
    ContentType("image/jpg") ~>
    ResponseBytes(buffer.toByteArray)
  }

  def intent = {
    case GET(Path("/")) =>
      Screenshot.take.map(stream).getOrElse(InternalServerError)
  }
}

object Main extends App {
  Http(8080).filter(DesktopImage).run()
}
