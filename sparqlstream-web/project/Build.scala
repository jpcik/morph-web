import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "sparqlstream-web"
    val appVersion      = "1.0.6"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "es.upm.fi.oeg.morph.streams" % "adapter-esper" % "1.0.8",
      "es.upm.fi.oeg.morph.streams" % "adapter-gsn" % "1.0.2",
      ("es.upm.fi.oeg.morph.streams" % "wrappers" % "1.0.3")
      .exclude("org.slf4j", "slf4j-log4j12")
        //"ch.qos.logback" % "logback-classic" % "1.0.9"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here 
        scalacOptions ++= Seq("-feature"),
        resolvers ++= Seq(
         //DefaultMavenRepository,         
         //("Local ivy Repository" at "file://"+Path.userHome.absolutePath+"/.ivy2/local")
         Resolver.url("local ivy",url("file://"+Path.userHome.absolutePath+"/.ivy2/local"))(Resolver.ivyStylePatterns),
         "aldebaran-libs" at "http://aldebaran.dia.fi.upm.es/artifactory/sstreams-external-libs-local",
         "aldebaran-releases" at "http://aldebaran.dia.fi.upm.es/artifactory/sstreams-releases-local",
         "plord" at "http://homepages.cs.ncl.ac.uk/phillip.lord/maven"
         //"jpc-repo" at "https://github.com/jpcik/jpc-repo/raw/master/repo"   
        )        
    )


}
