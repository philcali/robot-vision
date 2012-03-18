package capture
package server

object Resource {
  def unapply(file: String) = {
    try {
      val s = getClass.getResourceAsStream("/" + file)
      val bs = new java.io.ByteArrayOutputStream()
      pump(s, bs)

      Some(bs.toByteArray)
    } catch {
      case _ => None
    }
  }

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

  def retrieveBytes(file: String) =
    this.unapply(file).getOrElse(Array[Byte]())
}
