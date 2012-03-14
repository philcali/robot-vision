import sbt._
import Keys._

import less.Plugin.{
  LessKeys,
  lessSettings
}

object General {
  val settings: Seq[Setting[_]] = Defaults.defaultSettings ++ Seq(
    scalaVersion := "2.9.1",
    version := "0.0.1",
    organization := "com.github.philcali"
  )
}

object Server {
  val unfilteredVersion = SettingKey[String]("unfiltered-version")

  val settings: Seq[Setting[_]] = General.settings ++ Seq(
    unfilteredVersion := "0.6.0",
    libraryDependencies <++= (unfilteredVersion) { uv => Seq(
      "com.github.philcali" %% "lmxml-html" % "0.1.1",
      "com.github.philcali" %% "lmxml-json" % "0.1.1",
      "net.databinder" %% "unfiltered-filter" % uv,
      "net.databinder" %% "unfiltered-netty-server" % uv,
      "net.databinder" %% "unfiltered-netty-websockets" % uv
    ) }
  )
}

object Less {
  val copy = TaskKey[Seq[File]]("copy-bootstrap-js")

  val destJs = SettingKey[File]("copy-javascript-directory")

  val destPng = SettingKey[File]("copy-glyphs-directory")

  private def copyTask =
    (streams, destJs, destPng, sourceDirectory in (Compile, LessKeys.less)) map {
      (s, js, png, source) =>
        val perform = (oldFile: File, newFile: File) => {
          if (!newFile.exists) {
            s.log.info("Copying %s to %s" format(oldFile, newFile))
            IO.copyFile(oldFile, newFile)
          }
          newFile
        }
        (source / "js" * "*.js").get.map(f => perform(f, js / f.name)) ++
        (source / "img" * "*.png").get.map(f => perform(f, png / f.name))
      }

  val settings: Seq[Setting[_]] = lessSettings ++ inConfig(Compile)(Seq(
    destJs <<= (resourceDirectory)(_ / "js"),
    destPng <<= (resourceDirectory)(_ / "img"),
    copy <<= copyTask,
    cleanFiles <+= destJs,
    cleanFiles <+= destPng,
    LessKeys.less <<= LessKeys.less dependsOn copy,
    (LessKeys.mini in LessKeys.less) := true,
    (LessKeys.filter in LessKeys.less) := "bootstrap.less",
    (resourceManaged in LessKeys.less) <<= (resourceDirectory)(_ / "css"),
    (sourceDirectory in LessKeys.less) <<= (baseDirectory)(_ / "bootstrap")
  ))
}

object CaptureBuild extends Build {
  lazy val root = Project(
    "capture",
    file("."),
    settings = General.settings
  ) aggregate (control, server, app)

  lazy val control = Project(
    "capture-control",
    file("control"),
    settings = General.settings ++ Seq(
      crossScalaVersions := Seq("2.8.1", "2.8.2", "2.9.0", "2.9.1")
    )
  )

  lazy val server = Project(
    "capture-server",
    file("server"),
    settings = Server.settings ++ Less.settings
  ) dependsOn control

  lazy val app = Project(
    "capture-app",
    file("app"),
    settings = General.settings ++ Seq(
      libraryDependencies <++= (sbtVersion) { sv => Seq(
        "org.clapper" %% "argot" % "0.3.5",
        "org.scala-tools.sbt" %% "launcher-interface" % sv % "provided"
      ) }
    )
  ) dependsOn server
}
