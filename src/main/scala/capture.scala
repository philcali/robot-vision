package capture

import unfiltered.request._
import unfiltered.response._

import unfiltered.jetty.Http

import java.awt.{
  Toolkit,
  Rectangle,
  Robot
}
import java.awt.image.RenderedImage

import javax.imageio.ImageIO

import java.io.ByteArrayOutputStream

import util.control.Exception.allCatch

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

    ImageIO.write(img, "jpg", buffer)

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
