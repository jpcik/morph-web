package setup

import play.api._
import es.upm.fi.oeg.morph.esper.EsperServer
import akka.actor.Props
import es.upm.fi.oeg.siq.wrapper.ApiWrapper
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

object Global extends GlobalSettings {
  lazy val esper = new EsperServer

  override def onStart(app: Application) {
    Logger.info("Application has started") 
    
    esper.startup
    new ApiWrapper("social",esper.system)
    new ApiWrapper("hl7",esper.system)
    new ApiWrapper("weathermap",esper.system)

  }  
  
  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
    esper.shutdown
  }  
    
  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful(InternalServerError(views.html.error("Error in your request",ex)))
            //views.html.index(List(ex.getMessage()),null))
  }
}