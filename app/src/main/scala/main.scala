package capture
package app

import unfiltered.netty.{
  Http,
  Https
}
import unfiltered.netty.async
import unfiltered.netty.cycle.{
  Plan,
  Planify
}

import unfiltered.request._
import unfiltered.response._
import org.jboss.netty.channel.ChannelHandler
import java.io.File

import server._

import org.clapper.argot._

class Mode(val name: String, val desc: String) {
  def output() = {
    val absolute = (0 to (30 - name.length) / 8).map(_ => "\t").mkString
    println((" %s%s%s").format(name, absolute, desc))
  }

  def matches(action: String) = name.split(",").map(_.trim).contains(action)
}

case object Run extends Mode("run, web", "Launches embedded server")
case object Record extends Mode("record", "Records actions only")
case object SetProp extends Mode("set, add", "Sets a vision property")
case object RemoveProp extends Mode("remove, rm", "Removes a vision property")
case object ListProp extends Mode("list, ls", "Lists vision properties")
case object Help extends Mode("actions, help", "Displays this list")

object ValidMode {
  def all = List(Run, Record, SetProp, RemoveProp, ListProp, Help)

  def unapply(action: String) = all.find(_.matches(action))
}

object Main {
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
    List("i", "inet-port"), "8080", "internet port"
  )

  val bind = parser.option[String](
    List("b", "bind-address"), "(0.0.0.0)", "bind address"
  )

  val noConnect = parser.flag[Boolean](
    List("n", "no-connect"),
    "don't serve up connection js (ideal if using Chrome extension to connect)"
  )

  val participant = parser.option[String](
    List("v", "viewer-password"), "<viewer password>",
    "separate password for the 'viewer' user (leave blank for open)"
  )

  val generateSecret = parser.flag[Boolean](
    List("g", "gen-secret"), "generate a secret key to be passed to socket program"
  )

  val jpegCamera = parser.flag[Boolean](
    List("j", "jpeg-camera"), "Serves image data via jpeg camera transport"
  )

  val frameRate = parser.option[Int](
    List("f", "framerate"), "framerate 30",
    "If in jpeg camera mode, push image data at specified framerate"
  )

  val clear = parser.flag[Boolean](
    List("c", "clear-keys"), "Clears stuck keyboard inputs."
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

  def authed(users: Option[Users], plan: async.Plan) =
    users.map(u => async.Planify(Auth(u)(plan.intent))).getOrElse(plan)

  def propertyChecks(file: File) = {
    if (!file.exists) {
      throw new ArgotUsageException("%s does not exists." format file)
    } else if (file.isDirectory) {
      throw new ArgotUsageException("%s is a directory." format file)
    }
    file
  }

  def readSslProperties(file: File) {
    println("[CONFIG] setting ssl props from %s" format file)
    val p = Properties.fromFile(file).load(System.getProperties)

    System.setProperties(p.properties)
  }

  def handleMode() {
    action.value.map {
      case Help =>
        ValidMode.all.foreach(_.output)
        throw new ArgotUsageException("Vision commands")
      case SetProp =>
        if (extras.value.size >= 2) {
          val values = extras.value.take(2)
          Properties.load.set(values(0), values(1)).save()
        }
        throw new ArgotUsageException("set values")
      case RemoveProp =>
        extras.value.foldLeft(Properties.load)(_.remove(_)).save()
        throw new ArgotUsageException("removed values")
      case ListProp =>
        import scala.io.Source.{fromFile => open}
        import util.control.Exception.allCatch
        allCatch.opt(open(Properties.file).getLines)
          .map(_.foreach(println))
          .getOrElse(println("No properties"))
        throw new ArgotUsageException("listing properties")
      case Record =>
        println("Press [ENTER] to kill recording")
        extras.value.headOption
          .map(capture.control.Record(_))
          .map { r => r.start(); Console.readLine; r.stop() }
        throw new ArgotUsageException("Finished recording")
      case _ => println("[CONFIG] Preparing server")
    }
  }

  def main(args: Array[String]) {
    try {
      parser.parse(args)

      handleMode()

      // TODO: remove this
      clear.value.map { _ =>
        import control._
        (1 to 222).map(k => Robot(_.keyRelease(KeyTranslate(k.toString))))
      }

      val listen = port.value.getOrElse(8080)
      val address = bind.value.getOrElse("0.0.0.0")

      val master = password.value.map { pass =>
        println("[CONFIG] Authed controller")
        ValidUser(user.value.getOrElse(""), pass)
      }

      val viewer = participant.value.map { pass =>
        println("[CONFIG] Authed viewer participants")
        ViewingUser(master, pass)
      }

      val pulse = jpegCamera.value.map { _ =>
        println("[CONFIG] Setting framerate")
        Pulse(frameRate.value.getOrElse(30))
      }

      val secret = generateSecret.value.map { _ =>
        println("[CONFIG] Wrote secret to %s" format Properties.file)
        PrivateKey.save()
      } getOrElse {
        PrivateKey.retrieve.getOrElse(PrivateKey.generate)
      }

      val vision = authed(viewer, pulse.map(_ => ImageStream).getOrElse(Vision))

      val connect = noConnect.value.map { _ =>
        println("[CONFIG] Without control scripts")
        List[ChannelHandler]()
      } getOrElse (
        List(authed(master, Connect(secret)))
      )

      // Always have RobotTalk and Vision
      val handlers = List(RobotTalk(secret), vision) ++ connect

      def buildServer() {
        // Https server requires extra system variables
        val s: unfiltered.util.RunnableServer = secured.value.map { _ =>
          keyStoreInfo.value
            .orElse(Some(Properties.file))
            .map(propertyChecks)
            .map(readSslProperties)

          handlers.foldLeft(Https(listen, address))(_.handler(_))
        } getOrElse {
          handlers.foldLeft(Http(listen, address))(_.handler(_))
        }

        s.run(_ => {
          pulse.map { p =>
            println("[CONFIG] Using jpeg stream")
            ImageService.start(); p.start()
          }
          println("[START] Embedded server at %s:%d" format(address, listen))
        }, _ => {
          pulse.map(_ => ImageService.stop())
        })
      }

      buildServer()

    } catch {
      case e: ArgotUsageException => println(e.message)
    }
  }
}

class Main extends xsbti.AppMain {
  case class Exit(code: Int) extends xsbti.Exit

  def run(configuration: xsbti.AppConfiguration) = {
    Main.main(configuration.arguments)
    Exit(0)
  }
}
