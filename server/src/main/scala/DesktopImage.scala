package capture
package server

object DesktopImage {
  val Pattern = """desktop_(\d+\.\d+)x(\d+\.\d+)_(\d\.\d+)_([p|n])\.jpg""".r

  def unapply(image: String) = image match {
    case Pattern(x, y, q, p) =>
      Some((x.toDouble, y.toDouble, q.toFloat, p == "p"))
    case _ => Some((1.0, 1.0, 0.2f, false))
  }
}
