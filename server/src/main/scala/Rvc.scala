package capture
package server

import unfiltered.netty.async
import java.io.File

import unfiltered.netty.{
  Http,
  Https
}

object Rvc {
  def authed(users: Option[Users], plan: async.Plan) =
    users.map(u => async.Planify(Auth(u)(plan.intent))).getOrElse(plan)

  def propertyChecks(file: File) = {
    if (!file.exists) {
      throw new RuntimeException("%s does not exists." format file)
    } else if (file.isDirectory) {
      throw new RuntimeException("%s is a directory." format file)
    }
    file
  }

  def readSslProperties(file: File) {
    println("[CONFIG] setting ssl props from %s" format file)
    val p = Properties.fromFile(file)

    val check = (name: String) =>
      if (p.get(name).isEmpty)
        throw new RuntimeException("Property '%s' is undefined" format name)

    Seq("netty.ssl.keyStore", "netty.ssl.keyStorePassword").map(check)

    p.list.filter(_._1.startsWith("netty.ssl")).foreach {
      case (k, v) => System.setProperty(k, v)
    }
  }
}

case class Rvc(
  secured: Boolean = false,
  user: Option[String] = None,
  password: Option[String] = None,
  viewer: Option[String] = None,
  port: Int = 8080,
  address: String = "0.0.0.0",
  noConnect: Boolean = false,
  jpeg: Boolean = false,
  framerate: Long = 10L,
  keyStoreInfo: Option[File] = None
) {
  val service = if (jpeg) Some(ImageStream(1000L / framerate)) else None

  val master = password.map(p => ValidUser(user.getOrElse(""), p))

  val secret = PrivateKey.retrieve.getOrElse(PrivateKey.generate)

  val handlers = List(RobotTalk(secret, service),
    Rvc.authed(viewer.map(ViewingUser(master, _)), service.getOrElse(Vision))
  ) ++ (if (!noConnect) List(Rvc.authed(master, Connect(secret))) else Nil)

  val server = if (secured) {
    keyStoreInfo
      .orElse(Some(Properties.file))
      .map(Rvc.propertyChecks)
      .map(Rvc.readSslProperties)

    (Https(port, address) /: handlers) (_.handler(_))
  } else {
    (Http(port, address) /: handlers) (_.handler(_))
  }

  def start() = try {
    service.map(_.start())
    server.start()
    Right(true)
  } catch {
    case e =>
      service.map(_.stop())
      // Explicit call to release resources on exception
      server.destroy()
      Left(e)
  }

  def stop() = {
    service.map(_.stop())
    server.stop()
  }
}
