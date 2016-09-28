name := "sparqlstream-web"

organization := "org.morph"
    
version := "1.0.9"

scalaVersion := "2.11.2"

crossPaths := false

lazy val sparqlstrweb = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
      "es.upm.fi.oeg.morph.streams" % "adapter-esper" % "1.0.12",
      "es.upm.fi.oeg.morph.streams" % "adapter-gsn" % "1.0.11",
      ("es.upm.fi.oeg.morph.streams" % "wrappers" % "1.0.11")
        .exclude("org.slf4j", "slf4j-log4j12")
    )

resolvers ++= Seq(
  DefaultMavenRepository,
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "lsir remote" at "http://osper.epfl.ch:8081/artifactory/gsn-release"
)



