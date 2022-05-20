import sbt.Keys.libraryDependencies

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"
/*
Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)*/

lazy val root = (project in file("."))
  .settings(
    name := "ds-project-giulianini-ay1920",
  )
  .aggregate(quCommonPresentation, quClient, quService, quSystemTesting, auth)


lazy val commonDependencies = Seq(
  "org.scalamock" %% "scalamock" % "5.2.0" % Test,
  "org.scalatest" %% "scalatest" % "3.2.11" % Test)

lazy val quCommonPresentation = (project in file("qu-common-presentation"))
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq("io.grpc" % "grpc-netty" % "1.45.0",
      "org.scala-lang" % "scala-reflect" % "2.13.8",
      "com.thesamet.scalapb" % "scalapb-runtime-grpc_2.13" % "0.11.10",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.2",
      "com.roundeights" % "hasher_2.12" % "1.2.0",
      "com.typesafe.play" %% "play-json" % "2.8.2",
      "io.leonard" % "play-json-traits_2.13" % "1.5.1",
      "io.jsonwebtoken" % "jjwt-api" % "0.11.5",
      "io.jsonwebtoken" % "jjwt-impl" % "0.11.5",
      "io.jsonwebtoken" % "jjwt-jackson" % "0.11.5",
      //"io.jsonwebtoken" % "jjwt" % "0.9.1", old version before splitting
      "jakarta.xml.bind" % "jakarta.xml.bind-api" % "2.3.2",
      "org.glassfish.jaxb" % "jaxb-runtime" % "2.3.2"
    )
  )
lazy val quClient = (project in file("qu-client"))
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq("io.grpc" % "grpc-netty" % "1.45.0",
      "org.scala-lang" % "scala-reflect" % "2.13.8",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.2",
      "io.monix" %% "monix" % "3.4.0"
    )
  )
  .dependsOn(quCommonPresentation)

lazy val quService = (project in file("qu-service"))
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq("io.grpc" % "grpc-netty" % "1.45.0",
      "org.scala-lang" % "scala-reflect" % "2.13.8",
      "com.thesamet.scalapb" % "scalapb-runtime-grpc_2.13" % "0.11.10",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.2",
    )
  )
  .dependsOn(quCommonPresentation)

lazy val quSystemTesting = (project in file("qu-system-testing"))
  .settings(
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(quClient)
  .dependsOn(quService)

lazy val quDemo = (project in file("qu-demo"))
  .settings(
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(quCommonPresentation)
  .dependsOn(quClient)
  .dependsOn(quService)


lazy val auth = (project in file("auth"))
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value / "scalapb"
    ),
    libraryDependencies ++= commonDependencies ++ Seq("io.grpc" % "grpc-netty" % "1.45.0",
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,// % "protobuf",
      "org.scala-lang" % "scala-reflect" % "2.13.8",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.2",
      "io.monix" %% "monix" % "3.4.0"
    )
  )
  .dependsOn(quCommonPresentation)




