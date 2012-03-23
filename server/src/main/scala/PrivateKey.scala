package capture
package server

import java.io.{
  File,
  FileWriter
}

import java.math.BigInteger

import scala.io.Source.{fromFile => open}
import util.control.Exception.allCatch

object PrivateKey {
  val folder = new File(System.getProperty("user.home"), ".robot-vision")
  val file = new File(folder, "vision.sec")

  def retrieve = allCatch opt (open(file).getLines.next)

  def generate = {
    val random = new java.security.SecureRandom()
    new BigInteger(130, random).toString(32)
  }

  def save() = {
    if (!folder.exists) {
      folder.mkdirs()
    }

    val secret = generate

    val writer = new FileWriter(file)
    writer.write(secret)
    writer.close()

    secret
  }
}
