import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "sparqlstream-web"
    val appVersion      = "1.0.1"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "es.upm.fi.oeg.morph.streams" % "adapter-gsn" % "1.0.1"

    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here 
        resolvers ++= Seq(
         //DefaultMavenRepository,         
         //("Local ivy Repository" at "file://"+Path.userHome.absolutePath+"/.ivy2/local")
         Resolver.url("local ivy",url("file://"+Path.userHome.absolutePath+"/.ivy2/local"))(Resolver.ivyStylePatterns),
         "aldebaran-libs" at "http://aldebaran.dia.fi.upm.es/artifactory/sstreams-external-libs-local",
         "aldebaran-releases" at "http://aldebaran.dia.fi.upm.es/artifactory/sstreams-releases-local"
         //"jpc-repo" at "https://github.com/jpcik/jpc-repo/raw/master/repo"   
        )
    )

}
