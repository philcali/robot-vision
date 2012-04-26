package capture
package ui

import server.Properties

import scala.swing._
import event.{ EditDone, ButtonClicked }

import java.io.File

import java.awt.Color
import util.control.Exception.allCatch

object WebPanel extends GridPanel(10, 2) {
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
      allCatch.opt(
        server.Rvc(
          port = port.text.toInt,
          address = bind.text,
          secured = https.selected,
          noConnect = chrome.selected,
          user = if (username.text.isEmpty) None else Some(username.text),
          password = if (password.text.isEmpty) None else Some(password.text),
          viewer = if (viewer.text.isEmpty) None else Some(viewer.text),
          framerate = framerate.text.toLong,
          jpeg = jpeg.selected
      )).map(rvc = _).getOrElse(
        RvcTray.message("Web", "Bad Settings - previous settings",TrayMessage.Error)
      )
      rvc.start().fold ({ e =>
        RvcTray.message("Web", e.getMessage(), TrayMessage.Error)
      }, { _ =>
        launch.enabled = false
        RvcTray.message("Web", "RVC is running", TrayMessage.None)
        stop.enabled = true
      })
    case ButtonClicked(`stop`) =>
      rvc.stop()
      stop.enabled = false
      RvcTray.message("Web", "RVC has stopped", TrayMessage.None)
      launch.enabled = true
  }
}

