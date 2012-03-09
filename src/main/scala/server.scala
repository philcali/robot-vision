package capture
package server

import control._

import unfiltered.netty.websockets._

object RobotTalk extends Plan with CloseOnException {
  def intent = {
    case _ => {
      case Message(s, Text(msg)) => msg match {
        case KeyUp(keycode) =>
          Robot(_.keyRelease(KeyTranslate(keycode)))
        case KeyDown(keycode) =>
          Robot(_.keyPress(KeyTranslate(keycode)))
        case MouseUp(button) =>
          Robot(_.mouseRelease(MouseTranslate(button)))
        case MouseDown(button) =>
          Robot(_.mousePress(MouseTranslate(button)))
        case MouseMove(xStr, yStr) =>
          Robot(_.mouseMove(xStr.toInt, yStr.toInt))
      }
    }
  }
  def pass: Plan.PassHandler = (_.sendUpstream(_)) 
}
