package capture
package server

import control._

import unfiltered.Cookie
import unfiltered.netty.websockets._

class EventParsing(val ident: String) {
  def unapplySeq(text: String) = {
    if (!text.startsWith(ident)) None else
      Some(text.split("\\|").drop(1).toSeq)
  }
}

object MouseDown extends EventParsing("mousedown")
object MouseUp extends EventParsing("mouseup")
object MouseMove extends EventParsing("mousemove")

object KeyUp extends EventParsing("keyup")
object KeyDown extends EventParsing("keydown")

object RobotAuth extends EventParsing("robotauth")

object RobotTalk extends Plan with CloseOnException {
  def intent = {
    case _ => {
      case Message(s, Text(msg)) => msg match {
        case KeyUp(KeyTranslate(keyCode)) =>
          Robot(_.keyRelease(keyCode))
        case KeyDown(KeyTranslate(keyCode)) =>
          Robot(_.keyPress(keyCode))
        case MouseUp(MouseTranslate(button)) =>
          Robot(_.mouseRelease(button))
        case MouseDown(MouseTranslate(button)) =>
          Robot(_.mousePress(button))
        case MouseMove(xStr, yStr) =>
          Robot(_.mouseMove(xStr.toInt, yStr.toInt))
      }
    }
  }
  def pass: Plan.PassHandler = (_.sendUpstream(_)) 
}
