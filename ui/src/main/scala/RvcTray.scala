package capture
package ui

import scala.swing.Swing

import java.awt.{ SystemTray, TrayIcon, PopupMenu }
import javax.imageio.ImageIO
import util.control.Exception.allCatch

object TrayMessage {
  type Type = TrayIcon.MessageType

  val None = TrayIcon.MessageType.NONE
  val Info = TrayIcon.MessageType.INFO
  val Error = TrayIcon.MessageType.ERROR
  val Warn = TrayIcon.MessageType.WARNING
}

object RvcTray {
  def message(cap: String, msg: String, t: TrayMessage.Type = TrayMessage.None) {
    icon.foreach(_.displayMessage(cap, msg, t))
  }

  val image = ImageIO.read(getClass.getResourceAsStream("/icon_border.png"))

  lazy val icon = if (SystemTray.isSupported()) {
    val t = SystemTray.getSystemTray()

    val popup = new PopupMenu()
    val i = new TrayIcon(image, RvcWindow.appTitle, popup)
    i.setImageAutoSize(true)

    val webMenu = new java.awt.Menu("Web")
    val webStart = new java.awt.MenuItem("Start")
    webStart.addActionListener(Swing.ActionListener { e =>
      WebPanel.launch.doClick()
      i.setToolTip("%s (%s)" format (RvcWindow.appTitle, "Web Running"))
    })
    webMenu.add(webStart)

    val webStop = new java.awt.MenuItem("Stop")
    webStop.addActionListener(Swing.ActionListener { e =>
      WebPanel.stop.doClick()
      i.setToolTip(RvcWindow.appTitle)
    })
    webMenu.add(webStop)

    val recMenu = new java.awt.Menu("Recording")
    val recStart = new java.awt.MenuItem("Start")
    recStart.addActionListener(Swing.ActionListener { e =>
      RecordPanel.start.doClick()
      i.setToolTip("%s (%s)" format (RvcWindow.appTitle, "Recording Running"))
    })
    recMenu.add(recStart)

    val recStop = new java.awt.MenuItem("Stop")
    recStop.addActionListener(Swing.ActionListener { e =>
      RecordPanel.stop.doClick()
      i.setToolTip(RvcWindow.appTitle)
    })
    recMenu.add(recStop)

    val quit = new java.awt.MenuItem("Quit")
    quit.addActionListener(Swing.ActionListener { e =>
      WebPanel.stop.doClick()
      RecordPanel.stop.doClick()
      t.remove(i)
      sys.exit(0)
    })
    popup.add(webMenu)
    popup.add(recMenu)
    popup.add(quit)

    t.add(i)
    Some(i)
  } else {
    None
  }
}
