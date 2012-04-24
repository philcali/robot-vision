package capture
package ui

import server.Properties

import scala.swing._
import event.{
  EditDone,
  ButtonClicked
}

import java.io.File

import java.awt.{ SystemTray, TrayIcon, PopupMenu }
import java.awt.event.{ ActionEvent, ActionListener, MouseListener, MouseEvent }
import javax.imageio.ImageIO


case class PropertyConfigured(h: File) extends GridPanel(10, 2) {
  hGap = 3
  vGap = 5

  border = Swing.EmptyBorder(15, 10, 10, 15)

  private var rvc = app.Rvc()
  private var running = false

  val props = if (h.exists) Properties.fromFile(h) else Properties.load()

  case class PText(pname: String, default: String = "")
    extends TextField(props.get(pname).getOrElse(default), 10)

  case class PCheck(pname: String) extends CheckBox { self =>
    self.selected = props.get(pname).map(_ == "true").getOrElse(false)
  }

  object https extends PCheck("https")
  object jpeg extends PCheck("jpeg")
  object chrome extends PCheck("chrome")
  object username extends PText("username")
  object password extends PText("password")
  object bind extends PText("address", "0.0.0.0")
  object port extends PText("port", "8080")
  object viewer extends PText("viewer")
  object framerate extends PText("framerate", "10")

  object launch extends Button("Run")
  object stop extends Button("Stop") {
    enabled = false
  }

  contents ++ List(
    new Label("https"), https,
    new Label("Username"), username,
    new Label("Password"), password,
    new Label("Bind Address"), bind,
    new Label("Port"), port,
    new Label("Chrome Connect"), chrome,
    new Label("Viewer Password"), viewer,
    new Label("JPEG camera"), jpeg,
    new Label("Frame-rate"), framerate,
    launch, stop
  )

  listenTo(
    https, jpeg, chrome, username, password,
    bind, port, viewer, framerate, launch, stop
  )

  reactions += {
    case EditDone(property: PText) =>
      props.set(property.pname, property.text).save(h)
    case ButtonClicked(property: PCheck) =>
      props.set(property.pname, property.selected.toString).save(h)
    case ButtonClicked(`launch`) =>
      props.save(h)
      rvc = app.Rvc(
        port = port.text.toInt,
        address = bind.text,
        secured = https.selected,
        noConnect = chrome.selected,
        user = if (username.text.isEmpty) None else Some(username.text),
        password = if (password.text.isEmpty) None else Some(password.text),
        viewer = if (viewer.text.isEmpty) None else Some(viewer.text),
        framerate = framerate.text.toLong,
        jpeg = jpeg.selected
      )
      rvc.start()
      launch.enabled = false
      RvcConfigure.tray.foreach(_.displayMessage(
        "Web", "RVC is running", TrayIcon.MessageType.NONE
      ))
      stop.enabled = true
    case ButtonClicked(`stop`) =>
      rvc.stop()
      stop.enabled = false
      RvcConfigure.tray.foreach(_.displayMessage(
        "Web", "RVC has stopped", TrayIcon.MessageType.NONE
      ))
      launch.enabled = true
  }
}

object RvcConfigure extends SimpleSwingApplication {
  val appTitle = "Robot Vision Control"

  val web = new PropertyConfigured(new File(Properties.folder, "history"))

  def loadImage(name: String) =
    ImageIO.read(getClass.getResourceAsStream("/%s.png" format name))

  val tray = if (SystemTray.isSupported()) {
    val t = SystemTray.getSystemTray()

    val popup = new PopupMenu()
    val icon = new TrayIcon(loadImage("icon_border"), appTitle, popup)
    icon.setImageAutoSize(true)

    val start = new java.awt.MenuItem("Start")
    start.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) {
        web.launch.doClick()
        icon.setToolTip("%s (%s)" format (appTitle, "Running"))
      }
    })

    val stop = new java.awt.MenuItem("Stop")
    stop.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) {
        web.stop.doClick()
        icon.setToolTip(appTitle)
      }
    })

    val quit = new java.awt.MenuItem("Quit")
    quit.addActionListener(new java.awt.event.ActionListener {
      def actionPerformed(e: ActionEvent) {
        // Make sure rvc closes
        web.stop.doClick()
        t.remove(icon)
        sys.exit(0)
      }
    })
    popup.add(start)
    popup.add(stop)
    popup.add(quit)

    t.add(icon)
    Some(icon)
  } else None

  object RvcWindow extends Frame { inner =>
    val props = Properties.load()

    override def closeOperation = if (tray.isDefined) {
      inner.close()
    } else {
      web.stop.doClick()
      sys.exit(0)
    }

    tray.foreach(_.addMouseListener(new MouseListener {
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

    title = appTitle

    iconImage = loadImage("icon")

    centerOnScreen()

    contents = new TabbedPane {
      pages += new TabbedPane.Page("Web", web)
    }
  }

  def top = RvcWindow
}
