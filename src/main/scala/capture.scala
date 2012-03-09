package capture

import unfiltered.netty.Http
import unfiltered.request._
import unfiltered.response._

import server._

object Main extends App {
  Http(8080).handler(RobotTalk).handler(Connect).run()
}
