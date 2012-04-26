package capture
package ui

import scala.swing._
import event.ButtonClicked

import java.awt.Color

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
