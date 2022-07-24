addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.3")
//addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.3")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.2.0")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.1"