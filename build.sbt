import sbt.Keys.libraryDependencies

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "ds-project-giulianini-ay1920",
  )
  .aggregate(quCommonPresentation, quClient, quService, quSystemTesting)

lazy val quCommonPresentation = (project in file("qu-common-presentation"))
  .settings(
    libraryDependencies ++= Seq("io.grpc" % "grpc-netty" % "1.45.0",
      "org.scala-lang" % "scala-reflect" % "2.13.8",
      "com.thesamet.scalapb" % "scalapb-runtime-grpc_2.13" % "0.11.10",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.2")
  )
lazy val quClient = (project in file("qu-client"))
  .settings(
    libraryDependencies ++= Seq("io.grpc" % "grpc-netty" % "1.45.0",
      "org.scala-lang" % "scala-reflect" % "2.13.8",
      "com.thesamet.scalapb" % "scalapb-runtime-grpc_2.13" % "0.11.10",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.2")
  )
  .dependsOn(quCommonPresentation)
lazy val quService = (project in file("qu-service"))
  .settings(
    libraryDependencies ++= Seq("io.grpc" % "grpc-netty" % "1.45.0",
      "org.scala-lang" % "scala-reflect" % "2.13.8",
      "com.thesamet.scalapb" % "scalapb-runtime-grpc_2.13" % "0.11.10",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.2")
  )
  .dependsOn(quCommonPresentation)
lazy val quSystemTesting = (project in file("qu-system-testing"))
  .dependsOn(quClient)
  .dependsOn(quService)







