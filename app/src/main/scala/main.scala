package capture

import unfiltered.netty.{
  Http,
  Https
}
import unfiltered.request._
import unfiltered.response._

import server._

import org.clapper.argot._

object Main {
  import ArgotConverters._

  val preUsage = "Remote Control: Version 0.1 Copyright(c) 2012, Philip M. Cali"

  val parser = new ArgotParser("control", preUsage=Some(preUsage))

  val secured = parser.flag[Boolean](
    List("s", "secured"), "https server"
  )

  val user = parser.option[String](
    List("u", "user"), "user", "user to auth"
  )

  val password = parser.option[String](
    List("p", "password"), "password", "password to auth"
  )

  val port = parser.option[Int](
    List("i", "inetport"), "port", "internet port (8080)"
  )

  def main(args: Array[String]) {
    try {
      parser.parse(args)

      val listen = port.value.getOrElse(8080)

      val server = secured.value.map(_ => Https(listen)).getOrElse(Http(listen))

      println(user.value)
      println(password.value)
    } catch {
      case e: ArgotUsageException => println(e.message)
    }
    Http(8080).handler(RobotTalk).handler(Connect).run()
  }
}
