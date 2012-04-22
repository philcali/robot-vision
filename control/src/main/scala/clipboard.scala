package capture
package control

// Scala
import util.control.Exception.allCatch

import java.awt.Toolkit

import java.awt.datatransfer.{
  DataFlavor,
  StringSelection
}

object Clipboard {
  lazy val system = Toolkit.getDefaultToolkit.getSystemClipboard

  def apply(text: String) = allCatch opt {
    system.setContents(new StringSelection(text), null)
  }

  def retrieve =
    allCatch.opt(system.getContents(None))
      .filter(_ != null)
      .filter(_.isDataFlavorSupported(DataFlavor.stringFlavor))
      .map(_.getTransferData(DataFlavor.stringFlavor).toString)
}
