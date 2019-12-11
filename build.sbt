name := "chord-algorithm"

version := "1.0"

scalaVersion := "2.12.10"
lazy val akkaVersion = "2.5.26"

fork in Test := true

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster"                        % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools"                  % akkaVersion,

  "com.typesafe.akka" %% "akka-slf4j"                          % akkaVersion,
  "ch.qos.logback"    %  "logback-classic"                     % "1.2.3",
  "com.typesafe.akka" %% "akka-persistence"                    % akkaVersion,

  // test dependencies
  "com.typesafe.akka" %% "akka-testkit"                        % akkaVersion              % "test",
  "org.scalatest"     %% "scalatest"                           % "3.0.1"                  % "test",
  "commons-io"        %  "commons-io"                          % "2.4"                    % "test")


mainClass in (Compile, run) := Some("com.ashessin.cs441.project.Main")