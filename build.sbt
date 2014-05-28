name := "bbhack-14"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.twitter4j"            % "twitter4j-stream" % "4.0.1",
  "com.netflix.rxjava"       % "rxjava-scala"     % "0.18.3",
  "net.databinder.dispatch" %% "dispatch-core"    % "0.11.1",
  "org.json4s"              %% "json4s-jackson"   % "3.2.9" exclude("org.scala-lang", "scalap"),
  "org.zeromq"               % "jeromq"           % "0.3.2",
  "com.clearspring.analytics" % "stream" % "2.7.0",
  "org.scalatest"           %%  "scalatest"       % "2.1.5",
  "org.scalacheck"          %%  "scalacheck"      % "1.11.3"
)

play.Project.playScalaSettings
