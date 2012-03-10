package capture

import unfiltered.netty.{
  Http,
  Https
}
import unfiltered.netty.cycle.{
  Plan,
  Planify
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
    List("s", "secured"), "https server (http)"
  )

  val user = parser.option[String](
    List("u", "user"), "user", "user to auth (None)"
  )

  val password = parser.option[String](
    List("p", "password"), "password", "password to auth (None)"
  )

  val port = parser.option[Int](
    List("i", "inet-port"), "port", "internet port (8080)"
  )

  val bind = parser.option[String](
    List("b", "bind-address"), "bind", "bind address (0.0.0.0)"
  )

  def authed(plan: Plan) = if (!password.value.isEmpty) {
    val u = user.value.getOrElse("")
    val p = password.value.get

    Planify(Auth(ValidUser(u, p))(plan.intent))
  } else {
    plan
  }

  def main(args: Array[String]) {
    try {
      parser.parse(args)

      val listen = port.value.getOrElse(8080)
      val address = bind.value.getOrElse("0.0.0.0")

      secured.value.map( _ =>
        Https(listen, address)
          .handler(RobotTalk)
          .handler(authed(Connect))
          .handler(authed(Vision))
          .run()
      ) getOrElse(
        Http(listen, address)
          .handler(RobotTalk)
          .handler(authed(Connect))
          .handler(authed(Vision))
          .run()
      )
    } catch {
      case e: ArgotUsageException => println(e.message)
    }
  }
}

