package capture
package server

import unfiltered.request._
import unfiltered.response._
import unfiltered.Cycle

trait Users {
  def auth(u: String, p: String): Boolean
}

case class ValidUser(user: String, pass: String) extends Users {
  def auth(u: String, p: String) = user == u && pass == p
}

case class ViewingUser(valid: Option[ValidUser], pass: String) extends Users {
  def auth(u: String, p: String) = valid.map(_.auth(u, p)).getOrElse(false) ||
  (u == "viewer" && p == pass)
}

case class Auth(users: Users) {
  def apply[A,B](intent: Cycle.Intent[A,B]) =
    Cycle.Intent[A,B] {
      case req @ BasicAuth(user, pass) if (users.auth(user, pass)) =>
        Cycle.Intent.complete(intent)(req)
      case _ =>
        Unauthorized ~> WWWAuthenticate("""Basic realm="/" """)
    }
}
