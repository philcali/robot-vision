package capture

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

import server._

import org.clapper.argot._

object Main {
  import ArgotConverters._

  val preUsage = "Remote Control: Version 0.1 Copyright(c) 2012, Philip M. Cali"

  val parser = new ArgotParser("control", preUsage=Some(preUsage))

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
    List("s", "secret"), "generate a secret key to be pass to socket program"
  )

  val jpegCamera = parser.flag[Boolean](
    List("j", "jpeg-camera"), "Serves image data via jpeg camera transport"
  )

  val frameRate = parser.option[Int](
    List("f", "framerate"), "framerate 30",
    "If in jpeg camera mode, push image data at specified framerate"
  )

  def authed(users: Option[Users], plan: async.Plan) =
    users.map(u => async.Planify(Auth(u)(plan.intent))).getOrElse(plan)

  def generateKey() {
    val path = new java.io.File(System.getProperty("user.home"), ".robo-vision")
    if (!path.exists) {
      path.mkdirs()
    }

    import java.math.BigInteger
    import java.io.FileWriter

    val writer = new FileWriter(new java.io.File(path, "vision.sec"))
    val random = new java.security.SecureRandom()

    writer.write(new BigInteger(130, random).toString(32))
    writer.close()
  }

  def main(args: Array[String]) {
    try {
      parser.parse(args)

      val listen = port.value.getOrElse(8080)
      val address = bind.value.getOrElse("0.0.0.0")

      val master = password.value.map { pass =>
        ValidUser(user.value.getOrElse(""), pass)
      }

      val viewer = participant.value.map { pass =>
        ViewingUser(master, pass)
      }

      val vision = jpegCamera.value.map(_ =>
        ImageStream).orElse(Some(Vision)).map(authed(viewer, _)).get

      val connect = noConnect.value.map(_ =>
        List[ChannelHandler]()
      ).getOrElse(
        List(authed(master, Connect))
      )

      // Always have RobotTalk and Vision
      val handlers = List(RobotTalk, vision) ++ connect

      def buildServer() {
        // Https server requires extra system variables
        secured.value.map( _ =>
          handlers.foldLeft(Https(listen, address))(_.handler(_)).run()
        ) getOrElse {
          handlers.foldLeft(Http(listen, address))(_.handler(_)).run()
        }
      }

      jpegCamera.value.orElse(Some(false)).map{ isJpeg =>
        if (isJpeg) {
          val service = ImageService(frameRate.value.getOrElse(30))
          service.start()
          buildServer()
          service ! Quit
        } else {
          buildServer()
        }
      }

    } catch {
      case e: ArgotUsageException => println(e.message)
    }
  }
}

