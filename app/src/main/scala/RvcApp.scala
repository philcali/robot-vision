package capture
package app

import java.io.File

import org.clapper.argot._

import server._

object RvcApp {
  import ArgotConverters._

  val preUsage = "Robot Vision Control: Version 0.1 Copyright(c) 2012, Philip M. Cali"

  val parser = new ArgotParser("rvc", preUsage=Some(preUsage))

  val secured = parser.flag[Boolean](
    List("s", "secured"), "https server (http)"
  )

  val user = parser.option[String](
    List("u", "user"), "<none>", "user to auth"
  )

  val password = parser.option[String](
    List("p", "password"), "<none>", "password to auth"
  )

  val port = parser.option[Int](
    List("i", "inet-port"), "8080", "Web server internet port"
  )

  val bind = parser.option[String](
    List("b", "bind-address"), "(0.0.0.0)", "Web server bind address"
  )

  val noConnect = parser.flag[Boolean](
    List("n", "no-connect"),
    "don't serve up connection js (ideal if using Chrome extension to connect)"
  )

  val participant = parser.option[String](
    List("v", "viewer-password"), "<viewer password>",
    "Separate password for the 'viewer' user (leave blank for open)"
  )

  val jpegCamera = parser.flag[Boolean](
    List("j", "jpeg-camera"), "Serves image data via jpeg camera transport"
  )

  val frameRate = parser.option[Long](
    List("f", "framerate"), "framerate 10 (per second)",
    "If in jpeg camera mode, push image data at specified framerate"
  )

  val keyStoreInfo = parser.option[java.io.File](
    List("k", "key-store"), "/path/to/ssl.properties",
    "To be used with --secured. This is the properties file containing netty ssl info."
  ) {
    (s, opt) => new File(s)
  }

  val action = parser.parameter[Mode]("action", "vision mode", false) {
    (s, opt) =>
    s match {
      case ValidMode(mode) => mode
      case _ => Help
    }
  }

  val extras = parser.multiParameter[String](
    "extras", "action parameters", true
  )

  def debug[A](msg: String)(contents: A) = {
    println("[CONFIG] %s" format msg)
    contents
  }

  def handleMode(mode: Mode): Unit = mode match {
    case Help =>
      ValidMode.all.foreach(_.output)
    case Clear =>
      println("[INFO] Clearing all stuck inputs")
      control.Robot.clearInputs()
    case Generate =>
      println("[SUCCESS] Wrote secret to %s" format Properties.file)
      println("[SUCCESS] Chrome key: %s" format PrivateKey.save())
    case SetProp =>
      if (extras.value.size >= 2) {
        val (Seq(key), rest) = extras.value.splitAt(1)
        Properties.load.set(key, rest.mkString(" ")).save()
      }
    case RemoveProp =>
      extras.value.foldLeft(Properties.load)(_.remove(_)).save()
    case ListProp =>
      import scala.io.Source.{fromFile => open}
      import util.control.Exception.allCatch
      allCatch.opt(open(Properties.file).getLines)
        .map(_.foreach(println))
        .getOrElse(println("No properties"))
    case Record =>
      extras.value.headOption
        .map(debug("Press [ENTER] to kill recording"))
        .map(new capture.control.Record(_) with PostOperation)
        .map { r => r.start(); Console.readLine; r.stop() }
        .orElse(throw new ArgotUsageException("Supply a destination directory"))
    case _ =>
      handleWeb()
  }

  def handleWeb() {
    println("[CONFIG] Preparing server")

    val rvc = Rvc(
      port = port.value.getOrElse(8080),
      address = bind.value.getOrElse("0.0.0.0"),
      user = user.value,
      password = password.value.map(debug("Authed controller")),
      viewer = participant.value.map(debug("Authed viewer participants")),
      jpeg = jpegCamera.value.map(debug("Setting framerate")).isDefined,
      framerate = frameRate.value.getOrElse(10L),
      noConnect = noConnect.value.map(debug("Without control scripts")).isDefined,
      secured = secured.value.isDefined,
      keyStoreInfo = keyStoreInfo.value
    )

    rvc.server.run(_ => {
      rvc.service.map { p =>
        println("[CONFIG] Using jpeg stream")
        p.start()
      }
      println("[START] Embedded server at %s:%d" format(rvc.address, rvc.port))
    }, _ => {
      rvc.service.map(_.stop())
    })
  }

  def main(args: Array[String]) {
    try {
      parser.parse(args)

      action.value.map(handleMode)
    } catch {
      case e: ArgotUsageException => println(e.message)
      case e: Exception => println("[ERROR]: %s" format e.getMessage)
    }
  }
}

class Main extends xsbti.AppMain {
  case class Exit(code: Int) extends xsbti.Exit

  def run(configuration: xsbti.AppConfiguration) = {
    RvcApp.main(configuration.arguments)
    Exit(0)
  }
}
