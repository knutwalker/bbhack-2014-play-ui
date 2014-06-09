name := "bbhack-14"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "org.twitter4j"            % "twitter4j-stream" % "4.0.1",
  "com.netflix.rxjava"       % "rxjava-scala"     % "0.19.0",
  "net.databinder.dispatch" %% "dispatch-core"    % "0.11.1",
  "org.json4s"              %% "json4s-jackson"   % "3.2.10" exclude("org.scala-lang", "scalap"),
  "org.webjars" % "flot"   % "0.8.3",
  "org.webjars" % "lodash" % "2.4.1-4"
)
