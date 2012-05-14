package capture
package ui

import server.Properties

import scala.swing._

import java.awt.event.{ MouseListener, MouseEvent }
import javax.imageio.ImageIO

trait PropertyMapper {
  val pname: String
  val props: Properties
}

case class PText(pname: String, default: String, props: Properties)
  extends TextField(props.get(pname).getOrElse(default), 10) with PropertyMapper

case class PPass(pname: String, props: Properties)
  extends PasswordField(props.get(pname).getOrElse(""), 10) with PropertyMapper

case class PCheck(pname: String, props: Properties) extends CheckBox with PropertyMapper {
  selected = props.get(pname).filter(_ == "true").isDefined
}

object RvcWindow extends SimpleSwingApplication {
  // First-timers
  if (!Properties.folder.exists) {
    // Safely generate properties file and key
    server.PrivateKey.save()
  }

  val appTitle = "Robot Vision Control"

  def top = new Frame { inner =>
    val props = Properties.load()

    override def closeOperation = if (RvcTray.icon.isDefined) {
      inner.close()
    } else {
      WebPanel.stop.doClick()
      RecordPanel.stop.doClick()
      sys.exit(0)
    }

    RvcTray.icon.foreach(_.addMouseListener(new MouseListener {
      def mouseClicked(e: MouseEvent) {
        if (e.getButton == MouseEvent.BUTTON1 && !inner.showing) {
          inner.open()
        }
      }
      def mouseEntered(e: MouseEvent) {}
      def mouseExited(e: MouseEvent) {}
      def mousePressed(e: MouseEvent) {}
      def mouseReleased(e: MouseEvent) {}
    }))

    resizable = false

    title = appTitle

    iconImage = ImageIO.read(getClass.getResourceAsStream("/icon.png"))

    contents = new BoxPanel(Orientation.Vertical) {
      contents ++ List(WebPanel, ChromePanel, RecordPanel)
    }
  }
}
