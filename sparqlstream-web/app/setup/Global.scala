package setup

import play.api._
import es.upm.fi.oeg.morph.esper.EsperServer
import akka.actor.Props
import es.upm.fi.oeg.siq.wrapper.ApiWrapper

object Global extends GlobalSettings {
  lazy val esper = new EsperServer

  override def onStart(app: Application) {
    Logger.info("Application has started") 
    
    esper.startup
    
    //val caller=esper.system.actorOf(Props(new ApiWrapper("emt1")),"EmtWrapper")

  }  
  
  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
    esper.shutdown
  }  
    
}