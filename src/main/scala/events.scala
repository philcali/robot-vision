package capture
package control

import java.awt.event.{
  InputEvent,
  KeyEvent
}

class EventParsing(val ident: String) {
  def unapplySeq(text: String) = {
    if (!text.startsWith(ident)) None else
      Some(text.split("\\|").drop(1).toSeq)
  }
}

object MouseTranslate {
  def apply(button: String) = button match {
    case "0" => InputEvent.BUTTON1_MASK
    case "2" => InputEvent.BUTTON3_MASK
    case "1" => InputEvent.BUTTON2_MASK
  }
}

object KeyTranslate {
  def apply(key: String) = key match {
    case "65" => KeyEvent.VK_A
    case "66" => KeyEvent.VK_B
    case "67" => KeyEvent.VK_C
    case "68" => KeyEvent.VK_D
    case "69" => KeyEvent.VK_E
    case "70" => KeyEvent.VK_F
    case "71" => KeyEvent.VK_G
    case "72" => KeyEvent.VK_H
    case "73" => KeyEvent.VK_I
    case "74" => KeyEvent.VK_J
    case "75" => KeyEvent.VK_K
    case "76" => KeyEvent.VK_L
    case "77" => KeyEvent.VK_M
    case "78" => KeyEvent.VK_N
    case "79" => KeyEvent.VK_O
    case "80" => KeyEvent.VK_P
    case "81" => KeyEvent.VK_Q
    case "82" => KeyEvent.VK_R
    case "83" => KeyEvent.VK_S
    case "84" => KeyEvent.VK_T
    case "85" => KeyEvent.VK_U
    case "86" => KeyEvent.VK_V
    case "87" => KeyEvent.VK_W
    case "88" => KeyEvent.VK_X
    case "89" => KeyEvent.VK_Y
    case "90" => KeyEvent.VK_Z
    case "13" => KeyEvent.VK_ENTER
    case "8" => KeyEvent.VK_BACK_SPACE
    case "9" => KeyEvent.VK_TAB
    case "32" => KeyEvent.VK_SPACE
    case "16" => KeyEvent.VK_SHIFT
    case "17" => KeyEvent.VK_CONTROL
    case "18" => KeyEvent.VK_ALT
    case "37" => KeyEvent.VK_LEFT
    case "38" => KeyEvent.VK_UP
    case "39" => KeyEvent.VK_RIGHT
    case "40" => KeyEvent.VK_DOWN
    case "48" => KeyEvent.VK_0
    case "49" => KeyEvent.VK_1
    case "50" => KeyEvent.VK_2
    case "51" => KeyEvent.VK_3
    case "52" => KeyEvent.VK_4
    case "53" => KeyEvent.VK_5
    case "54" => KeyEvent.VK_6
    case "55" => KeyEvent.VK_7
    case "56" => KeyEvent.VK_8
    case "57" => KeyEvent.VK_9
    case "191" => KeyEvent.VK_SLASH
    case "220" => KeyEvent.VK_BACK_SLASH
    case "219" => KeyEvent.VK_OPEN_BRACKET
    case "221" => KeyEvent.VK_CLOSE_BRACKET
    case "190" => KeyEvent.VK_PERIOD
    case "187" => KeyEvent.VK_EQUALS
    case "189" => KeyEvent.VK_MINUS
    case "192" => KeyEvent.VK_BACK_QUOTE
    case "222" => KeyEvent.VK_QUOTE
    case "188" => KeyEvent.VK_COMMA
    case "186" => KeyEvent.VK_SEMICOLON
    case _ => KeyEvent.CHAR_UNDEFINED
  }
}

object MouseDown extends EventParsing("mousedown")
object MouseUp extends EventParsing("mouseup")
object MouseMove extends EventParsing("mousemove")

object KeyUp extends EventParsing("keyup")
object KeyDown extends EventParsing("keydown")

