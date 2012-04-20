package capture
package server

import control.{LongRunning, Record}
import java.lang.ProcessBuilder
import java.io.{
  InputStream,
  OutputStream,
  InputStreamReader,
  BufferedReader
}

trait PostOperation extends LongRunning { self: Record =>
  def debug(command: String) = {
    println("[EXECUTING] %s" format command)
    command
  }

  def output(process: java.lang.Process) {
    val buffer = new Array[Byte](1024)

    def pump(in: BufferedReader): Unit = in.readLine match {
      case line: String => println(line); pump(in)
      case _ =>
    }

    pump(new BufferedReader(new InputStreamReader(process.getInputStream)))
    process.waitFor
  }

  override def stop() {
    super.stop()

    val pb = new ProcessBuilder()

    val props = Properties.load
    val dest = props.get("record.dest").getOrElse("""\.""")
    val delete = props.get("record.cleanup").filter(_.trim == "on")

    try {
      props.get("record.command")
        .map(_.replaceAll("""\{location\}""", location.getAbsolutePath))
        .map(_.replaceAll("""\{filename\}""", location.getName))
        .map(_.replaceAll("""\{dest\}""", dest))
        .map(_.split(";").map(_.trim).map(debug))
        .map(
          _.map(_.split(" "))
          .map(pb.command(_: _*))
          .map(_.start())
          .map(output)
        ).map(_ => delete.map { _ =>
          println("[CONFIG] Cleaning up %s" format location.getAbsolutePath)
          location
            .listFiles
            .filter(f => Valid.findFirstIn(f.getName).isDefined)
            .foreach(_.delete)
          location.delete
        })
    } catch {
      case e => println("[ERROR]: Post process - %s" format e.getMessage)
    }
  }
}
