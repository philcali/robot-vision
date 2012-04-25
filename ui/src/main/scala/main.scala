package capture
package ui

import control.Record
import server.{ Properties, PostOperation }

import scala.swing._
import event.{ EditDone, ButtonClicked }

import java.io.File

import java.awt.{ SystemTray, TrayIcon, PopupMenu, Color }
import java.awt.event.{ MouseListener, MouseEvent }
import javax.imageio.ImageIO
import util.control.Exception.allCatch

case class PText(pname: String, default: String, props: Properties)
  extends TextField(props.get(pname).getOrElse(default), 10)

case class PCheck(pname: String, props: Properties) extends CheckBox {
  selected = props.get(pname).filter(_ == "true").isDefined
}

object PropertyConfigured extends GridPanel(10, 2) {
  hGap = 3
  vGap = 5

  border = Swing.TitledBorder(Swing.LineBorder(Color.BLACK), "Web")

  private var rvc = server.Rvc()
  private var running = false

  val h = new File(Properties.folder, "history")
  val props = if (h.exists) Properties.fromFile(h) else Properties.load()

  object https extends PCheck("https", props) {
    tooltip = app.RvcApp.secured.description
  }
  object jpeg extends PCheck("jpeg", props) {
    tooltip = app.RvcApp.jpegCamera.description
  }
  object chrome extends PCheck("chrome", props) {
    tooltip = app.RvcApp.noConnect.description
  }
  object username extends PText("username", "", props) {
    tooltip = app.RvcApp.user.description
  }
  object password extends PText("password", "", props) {
    tooltip = app.RvcApp.password.description
  }
  object bind extends PText("address", "0.0.0.0", props) {
    tooltip = app.RvcApp.bind.description
  }
  object port extends PText("port", "8080", props) {
    tooltip = app.RvcApp.port.description
  }
  object viewer extends PText("viewer", "", props) {
    tooltip = app.RvcApp.participant.description
  }
  object framerate extends PText("framerate", "10", props) {
    tooltip = app.RvcApp.frameRate.description
  }

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
      rvc = server.Rvc(
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
      rvc.start().fold ({ e =>
        RvcConfigure.tray.foreach(_.displayMessage(
          "Web", e.getMessage(), TrayIcon.MessageType.ERROR
        ))
      }, { _ =>
        launch.enabled = false
        RvcConfigure.tray.foreach(_.displayMessage(
          "Web", "RVC is running", TrayIcon.MessageType.NONE
        ))
        stop.enabled = true
      })
    case ButtonClicked(`stop`) =>
      rvc.stop()
      stop.enabled = false
      RvcConfigure.tray.foreach(_.displayMessage(
        "Web", "RVC has stopped", TrayIcon.MessageType.NONE
      ))
      launch.enabled = true
  }
}

object ChromePanel extends BoxPanel(Orientation.Horizontal) {
  border = Swing.TitledBorder(Swing.LineBorder(Color.BLACK), "Chrome Key")

  val keyLabel = new Label(server.PrivateKey.retrieve.getOrElse(""))
  val keyButton = new Button("Generate") {
    maximumSize = new java.awt.Dimension(150, 40)
  }

  contents ++ List(keyLabel, keyButton)

  listenTo(keyButton)

  reactions += {
    case ButtonClicked(keyButton) =>
      keyLabel.text = server.PrivateKey.save()
  }
}

object RecordingView extends GridPanel(3, 2) {
  hGap = 3
  vGap = 5

  border = Swing.TitledBorder(Swing.LineBorder(Color.BLACK), "Recording")

  val props = Properties.load()

  private val path = new File(util.Properties.userHome, "rvc_recording")

  private val recorder = new Record(path.getAbsolutePath) with PostOperation

  object cleanup extends PCheck("record.cleanup", props)
  object command extends PText("record.command", "", props)

  object start extends Button("Record")
  object stop extends Button("Stop") {
    enabled = false
  }

  listenTo(start, stop)

  reactions += {
    case ButtonClicked(`cleanup`) =>
      props.set(cleanup.pname, cleanup.selected).save()
    case EditDone(`command`) =>
      props.set(command.pname, command.text).save()
    case ButtonClicked(`start`) =>
      allCatch opt(recorder.restart()) getOrElse(recorder.start())
      start.enabled = false
      stop.enabled = true
    case ButtonClicked(`stop`) =>
      recorder.stop()
      RvcConfigure.tray.foreach(_.displayMessage(
        "Recording", "Recording has stopped", TrayIcon.MessageType.NONE
      ))
      stop.enabled = false
      start.enabled = true
  }

  contents ++ List(
    new Label("Cleanup images"), cleanup,
    new Label("Command"), command,
    start, stop
  )
}

object RvcConfigure extends SimpleSwingApplication {
  val appTitle = "Robot Vision Control"

  def loadImage(name: String) =
    ImageIO.read(getClass.getResourceAsStream("/%s.png" format name))

  val tray = if (SystemTray.isSupported()) {
    val t = SystemTray.getSystemTray()

    val popup = new PopupMenu()
    val icon = new TrayIcon(loadImage("icon_border"), appTitle, popup)
    icon.setImageAutoSize(true)

    val webMenu = new java.awt.Menu("Web")
    val webStart = new java.awt.MenuItem("Start")
    webStart.addActionListener(Swing.ActionListener { e =>
      PropertyConfigured.launch.doClick()
      icon.setToolTip("%s (%s)" format (appTitle, "Web Running"))
    })
    webMenu.add(webStart)

    val webStop = new java.awt.MenuItem("Stop")
    webStop.addActionListener(Swing.ActionListener { e =>
      PropertyConfigured.stop.doClick()
      icon.setToolTip(appTitle)
    })
    webMenu.add(webStop)

    val recMenu = new java.awt.Menu("Recording")
    val recStart = new java.awt.MenuItem("Start")
    recStart.addActionListener(Swing.ActionListener { e =>
      RecordingView.start.doClick()
      icon.setToolTip("%s (%s)" format (appTitle, "Recording Running"))
    })
    recMenu.add(recStart)

    val recStop = new java.awt.MenuItem("Stop")
    recStop.addActionListener(Swing.ActionListener { e =>
      RecordingView.stop.doClick()
      icon.setToolTip(appTitle)
    })
    recMenu.add(recStop)

    val quit = new java.awt.MenuItem("Quit")
    quit.addActionListener(Swing.ActionListener { e =>
      PropertyConfigured.stop.doClick()
      RecordingView.stop.doClick()
      t.remove(icon)
      sys.exit(0)
    })
    popup.add(webMenu)
    popup.add(recMenu)
    popup.add(quit)

    t.add(icon)
    Some(icon)
  } else None

  object RvcWindow extends Frame { inner =>
    val props = Properties.load()

    override def closeOperation = if (tray.isDefined) {
      inner.close()
    } else {
      PropertyConfigured.stop.doClick()
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

    contents = new BoxPanel(Orientation.Vertical) {
      contents ++ List(PropertyConfigured, ChromePanel, RecordingView)
    }
  }

  def top = RvcWindow
}
