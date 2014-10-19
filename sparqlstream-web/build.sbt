name := "sparqlstream-web"

organization := "org.morph"
    
version := "1.0.9"

scalaVersion := "2.11.2"

crossPaths := false

lazy val sparqlstrweb = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
      "es.upm.fi.oeg.morph.streams" % "adapter-esper" % "1.0.11",
      "es.upm.fi.oeg.morph.streams" % "adapter-gsn" % "1.0.10",
      ("es.upm.fi.oeg.morph.streams" % "wrappers" % "1.0.10")
        .exclude("org.slf4j", "slf4j-log4j12")
    )

resolvers ++= Seq(
  DefaultMavenRepository,
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "lsir remote" at "http://planetdata.epfl.ch:8081/artifactory/remote-repos",
  "aldebaran-releases" at "http://aldebaran.dia.fi.upm.es/artifactory/sstreams-releases-local"
)

//         "aldebaran-libs" at "http://aldebaran.dia.fi.upm.es/artifactory/sstreams-external-libs-local",
//         "aldebaran-releases" at "http://aldebaran.dia.fi.upm.es/artifactory/sstreams-releases-local",
//         "plord" at "http://homepages.cs.ncl.ac.uk/phillip.lord/maven"
         //"jpc-repo" at "https://github.com/jpcik/jpc-repo/raw/master/repo"   
//        )        
//    )


//}



