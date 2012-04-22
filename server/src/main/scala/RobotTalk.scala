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

// Mouse-related
object MouseDown extends EventParsing("mousedown")
object MouseUp extends EventParsing("mouseup")
object MouseMove extends EventParsing("mousemove")

// Keyboard-related
object KeyUp extends EventParsing("keyup")
object KeyDown extends EventParsing("keydown")

// Other interaction
object RobotAuth extends EventParsing("auth")
object RobotRecord extends EventParsing("record")
object ClipboardRetrieve extends EventParsing("clipretrieve")
object ClipboardSet extends EventParsing("clipset")

case class RobotTalk(secret: String) extends Plan with CloseOnException {
  @volatile private var controller: Option[java.lang.Integer] = None

  // One per controller
  private var recorder = setupRecorder

  def setupRecorder() = {
    val name = "recording_%d" format System.currentTimeMillis
    val location = new java.io.File(System.getProperty("user.home"), name)

    new Record(location.getAbsolutePath) with PostOperation
  }

  def check(pred: java.lang.Integer => Boolean)(block: => Unit) {
    controller.map(i => if (pred(i)) block) 
  }

  // Extra protection
  def isController(socket: WebSocket) = {
    controller.map(_ == socket.channel.getId).getOrElse(false)
  }

  def intent = {
    case _ => {
      case Open(s) =>
        check(_ != s.channel.getId) {
          s.send("Already being controlled")
        }
      case Message(s, Text(msg)) => msg match {
        case RobotAuth(key) if controller.isEmpty =>
          if (key == secret) {
            controller = Some(s.channel.getId)
            s.send("auth|connect")
          } else {
            s.send("bad-key| - %s" format key)
          }
        case _ if isController(s) => RobotProtect.message(s, msg)
      }
      case Close(s) =>
        // Prevent runaways
        if (recorder.isRunning) recorder.stop()
        // Safely clear inputs on disconnect
        Robot.clearInputs()
        check(_ == s.channel.getId)(controller = None)
    }
  }
  def pass: Plan.PassHandler = (_.sendUpstream(_)) 

  object RobotProtect {
    def handleGeneral(s: WebSocket, msg: String) = msg match {
      case RobotRecord(value) => value match {
        case "record" if !recorder.isRunning =>
          recorder.start()
          s.send("record|started")
        case "stop" if recorder.isRunning =>
          recorder.stop()
          recorder = setupRecorder()
          s.send("record|stopped")
        case _ if recorder.isRunning => s.send("record|started")
        case _ => s.send("record|stopped")
      }
      case ClipboardRetrieve(value) =>
        s.send("clipboard|get|%s" format Clipboard.retrieve.getOrElse(""))
      case msg if msg.startsWith(ClipboardSet.ident) =>
        val value = ClipboardSet.unapplySeq(msg).map(_.mkString("|")).get
        if (Clipboard(value).isDefined) {
          s.send("clipboard|success")
        } else {
          s.send("clipboard|failure")
        }
      case _ =>
        handleInput(s, msg)
    }

    def handleInput(s: WebSocket, msg: String) = msg match {
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
      case _ =>
        println("[DEBUG]: Unknown msg: %s" format msg)
    }

    def message(s: WebSocket, msg: String) = handleGeneral(s, msg)
  }
}
