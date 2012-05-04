package capture
package server

import java.io.{
  File,
  FileInputStream,
  FileOutputStream
}
import java.util.{ Properties => JProps }

import util.control.Exception.allCatch
import collection.JavaConversions._

object Properties {
  val folder = new File(System.getProperty("user.home"), ".robot-vision")
  val file = new File(folder, "vision.properties")

  def load(): Properties = {
    if (!folder.exists) {
      folder.mkdir
      file.createNewFile
    }

    fromFile(file)
  }

  def fromFile(local: File) = {
    val props = new JProps()
    val ins = new FileInputStream(local)
    allCatch opt(props.load(ins))
    ins.close()
    new Properties(props)
  }
}

class Properties(wrapped: JProps) {
  def properties = new JProps(wrapped)

  def get(name: String) = {
    val prop = wrapped.getProperty(name)
    if (prop == null) None else Some(prop)
  }

  def set(name: String, value: Any) = {
    wrapped.setProperty(name, value.toString)
    this
  }

  def list: Map[String, String] =
    wrapped.entrySet.map(s => (s.getKey.toString, s.getValue.toString)).toMap

  def load(old: JProps) = {
    val props = properties
    old.entrySet.foreach { s =>
      props.setProperty(s.getKey.toString, s.getValue.toString)
    }
    new Properties(props)
  }

  def remove(name: String) = {
    wrapped.remove(name)
    this
  }

  def save(f: File = Properties.file) {
    if (!Properties.folder.exists) {
      Properties.folder.mkdirs()
    }

    val fis = new FileOutputStream(f)
    wrapped.store(fis, "") 
    fis.close()
  }
}
