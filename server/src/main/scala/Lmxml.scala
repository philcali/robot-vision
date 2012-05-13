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

import java.net.{ InetAddress => Inet }

import util.control.Exception.allCatch

trait Lmxml extends Conversion {
  import control.Robot

  lazy val hostname =
    allCatch opt (Inet.getLocalHost.getHostName) getOrElse "unknown-host"

  def createParser(step: Int) = new PlainLmxmlParser(step) with HtmlShortcuts

  def screenData = Seq(
    "desktop-name" -> Value(hostname),
    "width" -> Value(Robot.display.getWidth.toInt),
    "height" -> Value(Robot.display.getHeight.toInt)
  )

  def data: Seq[(String, Processor)]

  def index(source: String) = {
    val trans = Transform((screenData ++ data):_*)

    Ok ~> Html(convert(source)(trans andThen XmlConvert))
  }
}
