name := "capture"

version := "0.0.1"

organization := "com.github.philcali"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "1.3.2",
  "com.github.philcali" %% "lmxml-html" % "0.1.1",
  "com.github.philcali" %% "lmxml-json" % "0.1.1",
  "net.databinder" %% "unfiltered-filter" % "0.6.0",
  "net.databinder" %% "unfiltered-jetty" % "0.6.0",
  "net.databinder" %% "unfiltered-netty-server" % "0.6.0",
  "net.databinder" %% "unfiltered-netty-websockets" % "0.6.0"
)
