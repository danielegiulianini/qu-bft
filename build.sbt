ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "ds-project-giulianini-ay1920"
  )

libraryDependencies += "io.grpc" % "grpc-netty" % "1.45.0"
libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.13.8"
libraryDependencies += "com.thesamet.scalapb" % "scalapb-runtime-grpc_2.13" % "0.11.10"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.2"

