import sbt._
import Keys._

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
    settings = Server.settings
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