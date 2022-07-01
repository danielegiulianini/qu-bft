import sbt.Keys.{libraryDependencies, mainClass}

ThisBuild / organization := "org.unibo"

ThisBuild / version := "1.0.0"

ThisBuild / scalaVersion := "2.13.8"


lazy val root = (project in file("."))
  .settings(
    name := "ds-project-giulianini-ay1920",
  )
  .dependsOn(quDemo)
  .aggregate(quCore, quStorage, quCommunication, quPresentation, quClient, quService, quSystemTesting, quDemo, auth)


lazy val jwtDep = Seq("io.jsonwebtoken" % "jjwt-api" % "0.11.5",
  "io.jsonwebtoken" % "jjwt-impl" % "0.11.5",
  "io.jsonwebtoken" % "jjwt-jackson" % "0.11.5"
)

coverageEnabled.in(ThisBuild, Test, test) := true

lazy val commonDependencies = Seq(
  "org.scalamock" %% "scalamock" % "5.2.0" % Test,
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  "org.scalacheck" % "scalacheck_2.13" % "1.16.0",
  "org.scalatestplus" %% "scalacheck-1-16" % "3.2.12.0" % "test"
  //,  "org.scoverage" %% "scalac-scoverage-runtime" % "1.0.4"
)


lazy val auth = (project in file("auth"))
  .settings(
    Compile / PB.targets := Seq( //PB.targets in Compile := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb" // scalapb.gen() -> (sourceManaged in Compile).value / "scalapb"
    ),
    libraryDependencies ++= commonDependencies ++ jwtDep ++ Seq("io.grpc" % "grpc-netty" % "1.45.0",
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion, // % "protobuf",
      "org.scala-lang" % "scala-reflect" % "2.13.8",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.3",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
    )
  )

lazy val quCore = (project in file("qu-core"))
  .settings(
    libraryDependencies ++= commonDependencies //++ Seq("com.roundeights" %% "hasher" % "1.2.1"//Seq("com.roundeights" % "hasher_2.12" % "1.2.0"
  )

//to be renamed
lazy val quPresentation = (project in file("qu-common-presentation"))
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq("io.grpc" % "grpc-netty" % "1.45.0",
      "org.scala-lang" % "scala-reflect" % "2.13.8",
      "com.thesamet.scalapb" % "scalapb-runtime-grpc_2.13" % "0.11.10",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.3"
     /* "com.typesafe.play" %% "play-json" % "2.8.2",
      "io.leonard" % "play-json-traits_2.13" % "1.5.1",
      "jakarta.xml.bind" % "jakarta.xml.bind-api" % "2.3.2",
      "org.glassfish.jaxb" % "jaxb-runtime" % "2.3.2"*/
    )
  )
  .dependsOn(quCore % "compile->compile;test->test")


lazy val quCommunication = (project in file("qu-communication"))
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq("io.monix" %% "monix" % "3.4.0"
    )
  )
  .dependsOn(quPresentation)
  .dependsOn(auth)
  .dependsOn(quCore % "compile->compile;test->test")


lazy val quClient = (project in file("qu-client"))
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq("io.grpc" % "grpc-netty" % "1.45.0",
      "org.scala-lang" % "scala-reflect" % "2.13.8",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.2"
    )
  )
  .dependsOn(quCore % "compile->compile;test->test")
  .dependsOn(auth)
  .dependsOn(quCommunication)
  .dependsOn(quPresentation)

lazy val quService = (project in file("qu-service"))
  .settings(
    libraryDependencies ++= commonDependencies ++ jwtDep ++
      Seq("io.grpc" % "grpc-netty" % "1.45.0",
      "org.scala-lang" % "scala-reflect" % "2.13.8",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.2"
    )
  )
  .dependsOn(quCore % "compile->compile;test->test")
  .dependsOn(quCommunication)
  .dependsOn(quPresentation)
  .dependsOn(quStorage)


lazy val quStorage = (project in file("qu-storage"))
  .settings(
    libraryDependencies ++= commonDependencies ++ Seq("org.scala-lang" % "scala-reflect" % "2.13.8"
    )
  )
  .dependsOn(quCore)

lazy val quSystemTesting = (project in file("qu-system-testing"))
  .settings(
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(quCore % "compile->compile;test->test")
  .dependsOn(quClient)
  .dependsOn(quService % "compile->compile;test->test")


lazy val quDemo = (project in file("qu-demo"))
  .settings(
    libraryDependencies ++= commonDependencies,
    Compile / mainClass := Some("qu.controller.Demo")
  )
  .dependsOn(quPresentation)
  .dependsOn(quClient)
  .dependsOn(quService % "compile->compile;test->test")

Compile / mainClass:= (quDemo / Compile / mainClass).value

//run in Compile <<= (run in Compile in quDemo)


