import sbt._
import Keys._

import sbtassembly.Plugin.{
  AssemblyKeys,
  MergeStrategy,
  assemblySettings
}

import less.Plugin.{
  LessKeys,
  lessSettings
}

import sbtjslint.Plugin.{
  ShortFormatter,
  LintKeys,
  lintSettings
}

object General {
  val buildVersion = "0.0.3"

  val buildShellPrompt = (state: State) => {
    val projectId = Project.extract (state).currentProject.id
    val shortened = projectId.split("-").map(_.take(1)).mkString("-")
    "%#3s %s> " format (shortened, buildVersion)
  }

  val crossVersions = Seq(
    "2.8.1", "2.8.2", "2.9.0", "2.9.0-1", "2.9.1"
  )

  val settings: Seq[Setting[_]] = Defaults.defaultSettings ++ Seq(
    scalaVersion := "2.9.1",
    version := buildVersion,
    shellPrompt := buildShellPrompt,
    organization := "com.github.philcali",
    publishTo <<= version { v =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { x => false },
    pomExtra := (
      <url>https://github.com/philcali/robot-vision</url>
      <licenses>
        <license>
          <name>The MIT License</name>
          <url>http://www.opensource.org/licenses/mit-license.php</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:philcali/robot-vision.git</url>
        <connection>scm:git:git@github.com:philcali/robot-vision.git</connection>
      </scm>
      <developers>
        <developer>
          <id>philcali</id>
          <name>Philip Cali</name>
          <url>http://philcalicode.blogspot.com/</url>
        </developer>
      </developers>
    )
  )
}

object Server {
  val unfilteredVersion = SettingKey[String]("unfiltered-version")

  val settings: Seq[Setting[_]] = General.settings ++ Seq(
    unfilteredVersion := "0.6.2",
    crossScalaVersions := General.crossVersions,
    libraryDependencies <++= (unfilteredVersion) { uv => Seq(
      "com.github.philcali" %% "lmxml-html" % "0.1.1",
      "net.databinder" %% "unfiltered" % uv,
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

object Lint {
  val settings: Seq[Setting[_]] = lintSettings ++ inConfig(Compile)(Seq(
    (LintKeys.indent in LintKeys.jslint) := 2,
    (LintKeys.flags in LintKeys.jslint) := Seq(
      "undef", "browser", "on", "anon", "sloppy"
    ),
    (LintKeys.outputs in LintKeys.jslint) ~= (_.take(1)),
    (sourceDirectory in LintKeys.jslint) <<= (resourceDirectory)(_ / "lib"),
    (excludeFilter in LintKeys.jslint) := "jquery.js"
  ))
}

object Executable {
  import AssemblyKeys._

  val settings: Seq[Setting[_]] = assemblySettings ++ Seq(
    jarName in assembly <<= (version)("rvc-%s.jar".format(_: String)),
    target in assembly <<= (baseDirectory)(_.getParentFile),
    excludedFiles in assembly := { (bases: Seq[File]) =>
      bases flatMap { base =>
        (base / "META-INF" * "*").get collect {
          case f if f.name == "NOTICE.txt" => f
          case f if f.name.toLowerCase == "license.txt" => f
          case f if f.name.toLowerCase == "manifest.mf" => f
        }
      }
    }
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
      crossScalaVersions := General.crossVersions,
      libraryDependencies += "org.scalatest" %% "scalatest" % "1.7.1" % "test"
    )
  )

  lazy val server = Project(
    "capture-server",
    file("server"),
    settings = Server.settings ++ Less.settings ++ Lint.settings
  ) dependsOn control

  lazy val app = Project(
    "capture-app",
    file("app"),
    settings = General.settings ++ Seq(
      scalacOptions ++= Seq("-deprecation", "-unchecked"),
      libraryDependencies <++= (sbtVersion) { sv => Seq(
        "org.clapper" %% "argot" % "0.3.6",
        "org.scala-tools.sbt" %% "launcher-interface" % sv % "provided"
      ) }
    )
  ) dependsOn server

  lazy val ui = Project(
    "capture-ui",
    file("ui"),
    settings = General.settings ++ Executable.settings ++ Seq(
      scalacOptions ++= Seq("-deprecation", "-unchecked"),
      mainClass in (Compile, run) := Some("capture.ui.RvcWindow"),
      libraryDependencies <+= (scalaVersion) { sv =>
        "org.scala-lang" % "scala-swing" % sv
      }
    )
  ) dependsOn app
}
