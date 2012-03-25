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
  def retrieve = Properties.load.get("vision.sec")

  def generate = {
    val random = new java.security.SecureRandom()
    new BigInteger(130, random).toString(32)
  }

  def save() = {
    val secret = generate

    Properties.load.set("vision.sec", secret).save()

    secret
  }
}
