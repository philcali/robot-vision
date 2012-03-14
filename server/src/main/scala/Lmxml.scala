package capture
package server

import lmxml.{
  LmxmlConvert,
  Conversion,
  PlainLmxmlParser,
  XmlConvert
}
import lmxml.transforms._
import lmxml.shortcuts.html.HtmlShortcuts

import unfiltered.response._

import java.net.InetAddress

trait Lmxml extends Conversion {
  import control.Robot

  def createParser(step: Int) = new PlainLmxmlParser(step) with HtmlShortcuts

  def screenData = Seq(
    "desktop-name" -> Value(InetAddress.getLocalHost.getHostName),
    "width" -> Value(Robot.display.getWidth.toInt),
    "height" -> Value(Robot.display.getHeight.toInt)
  )

  def data: Seq[(String, Processor)]

  def index(source: String) = {
    val trans = Transform((screenData ++ data):_*)

    Ok ~> Html(convert(source)(trans andThen XmlConvert))
  }
}
