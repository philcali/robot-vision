package capture
package server

import unfiltered.request._
import unfiltered.response._

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
    new String(retrieveBytes(file), "UTF-8")
  }

  def retrieveBytes(file: String) = {
    val s = getClass.getResourceAsStream("/" + file)
    val bs = new java.io.ByteArrayOutputStream()
    pump(s, bs)

    bs.toByteArray
  }
}

trait DefaultPlan extends Plan with ThreadPool with ServerErrorResponse {
  val validFiles: List[String]

  object ValidJs {
    def unapply(file: String) = {
      if (validFiles.exists(_ == file)) Some(file) else None
    }
  }
}

object Connect extends DefaultPlan with Lmxml with ResourceLoader {
  import lmxml.transforms.If

  val validFiles = List("connect.js", "control.js")

  def data = Seq("connect-check" -> If (true)(Nil))

  def intent = {
    case Path("/desktop.html") =>
      index(retrieve("index.lmxml"))
    case Path(Seg(ValidJs(file) :: Nil)) =>
      Ok ~> ContentType("text/javascript") ~> ResponseString(retrieve(file))
  }
}
