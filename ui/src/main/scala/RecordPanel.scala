package capture
package ui

import control.Record
import server.{ Properties, PostOperation }

import scala.swing._
import event.{ EditDone, ButtonClicked }

import java.io.File

import java.awt.{ TrayIcon, Color }
import util.control.Exception.allCatch

object RecordPanel extends GridPanel(3, 2) {
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

  listenTo(cleanup, command, start, stop)

  reactions += {
    case ButtonClicked(clicker: PCheck) =>
      props.set(clicker.pname, clicker.selected).save()
    case EditDone(text: PText) =>
      props.set(text.pname, text.text).save()
    case ButtonClicked(`start`) =>
      allCatch opt(recorder.restart()) getOrElse(recorder.start())
      start.enabled = false
      stop.enabled = true
    case ButtonClicked(`stop`) =>
      recorder.stop()
      RvcTray.message("Recording", "Recording has stopped", TrayMessage.Info)
      stop.enabled = false
      start.enabled = true
  }

  contents ++ List(
    new Label("Cleanup images"), cleanup,
    new Label("Command"), command,
    start, stop
  )
}

