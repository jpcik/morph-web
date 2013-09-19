package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits._
import scala.collection.mutable.ArrayBuffer
import javax.xml.bind.JAXBContext
import java.io.StringWriter
import javax.xml.bind.Marshaller
import es.upm.fi.oeg.morph.stream.evaluate.QueryEvaluator
import java.net.URI
import java.util.Properties
import com.hp.hpl.jena.rdf.model.Model
import java.io.ByteArrayOutputStream
import es.upm.fi.oeg.morph.voc.RDFFormat
import es.upm.fi.oeg.morph.common.ParameterUtils
import collection.JavaConversions._
import es.upm.fi.oeg.siq.sparql.SparqlResults
import setup.Global
import es.upm.fi.oeg.morph.stream.evaluate.StreamReceiver
import es.upm.fi.oeg.morph.stream.evaluate.EvaluatorUtils
import play.api.libs.concurrent.Promise
import akka.actor.Actor
import scala.concurrent.duration._
import play.libs.Akka
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import akka.actor.PoisonPill
import language.postfixOps
import es.upm.fi.oeg.morph.stream.esper.EsperAdapter
import es.upm.fi.oeg.morph.stream.gsn.GsnAdapter

object Application extends Controller {
  
  val props= ParameterUtils.load(this.getClass().getClassLoader().getResourceAsStream("config/siq.properties"))
  val gsns=props.getProperty("gsn.endpoints").split(",")
  val mapGsns=gsns.map(g=>g->("mappings/"+g+".ttl",props.getProperty("gsn.endpoint."+g))).toMap
  val taskForm=Form(tuple("system"->nonEmptyText,"query"->nonEmptyText,"action"->nonEmptyText))
  val initForm=Form(tuple("system"->nonEmptyText,"query"->nonEmptyText))
  def index = Action {
    Ok(views.html.index(List("Your new application is ready."),initForm))
  }
 
  def dipp = WebSocket.async[String] { request => 
 Logger.debug("trilololo") 
  // Log events to the console
  val dd=Akka.system.actorOf(Props[Drop])

  val in = Iteratee.foreach[String](println).mapDone { _ =>
    println("Disconnected")
    dd ! Stop()
    dd ! PoisonPill
  }

//dd ! PoisonPill
 // Send a single 'Hello!' message
     implicit val timeout = Timeout(5 second)
     //val out=ask(dd,Subs()).mapTo[Enumerator[String]]
     (dd ? Subs()).map{
       case Accpt(e)=>(in,e)
     }
    //Enumerator("Helopopopolo!")
  
  //(in,Await.result(out,timeout.duration))
}
  
    
 def query=Action{implicit request =>
    taskForm.bindFromRequest.fold(
        errors =>BadRequest(views.html.query(  null,errors)),
        vals =>{
          Logger.debug("getting form "+vals)
          if (vals._3.equals("query")){ 
            val r =Sensor.query(vals._1,vals._2)
            Ok(views.html.result(r.asInstanceOf[SparqlResults],mapGsns(vals._1)._2,null))
          }
          else{
            val rec=new ResultsReceiver
            val r =Sensor.register(vals._1,vals._2)
            
            //Ok.stream(Enumerator.repeat("papa")) //( "wweq").andThen(Enumerator.eof))
            //Ok(views.html.result(null,mapGsns(vals._1)._2,null))
            Ok(views.html.qid(r,taskForm))
          }
            
        }
        )
  }

  def posequery(id:String)=Action{
    //if (!id.equals("citybikes")) BadRequest(views.html.index(null,null))
    Ok(views.html.query(id,taskForm))
  }
 
  def posequeryall=Action{
    Ok(views.html.query(null,taskForm))
  }
 class ResultsReceiver extends StreamReceiver{
   
  override def receiveData(s:SparqlResults){   
    Logger.debug("got: "+EvaluatorUtils.serializeJson(s))
    //val orig=(System.nanoTime-stTime)/1000000
    //logger.debug("got at: "+orig)
    //val timed:Long = (Math.round(orig/rounding)*rounding).toLong
    //logger.info("times: "+orig+" "+timed)
    //allResults.+= ((timed,s))
  }
}

  
}

case class Accpt(e:Enumerator[String]) 
case class Msg(m:String)
case class Subs()
case class Stop()

class Drop extends Actor{
 
  lazy val schedule={
    import context.dispatcher
    context.system.scheduler.schedule(0 seconds, 5 seconds){
      println("charafa")
      self ! Msg("ralalala")
      
    }
  }
  
  val (enum,channel)=Concurrent.broadcast[String]
  def receive={
    case Subs()=>
      schedule
      sender ! Accpt(enum)
    case Msg(m)=>boradcast(m)
    case Stop()=>schedule.cancel
    case p=> println("mjmjm"+p)
  }
  
  def boradcast(m:String)={
    channel.push(m)
  }
}

object Sensor{
  val props= ParameterUtils.load(getClass.getClassLoader().getResourceAsStream("config/siq.properties"))
  val sensors=new ArrayBuffer[String]
  def all():List[String]=sensors.toList
  def query(system:String,query:String)={
    val (mapping,uri)=Application.mapGsns(system)
    println("got "+mapping)
    val props1= new Properties
    props1++=props
    props1.put("gsn.endpoint",uri)
    
    //val gsn=new EsperAdapter(Global.esper.system)
    val gsn=new GsnAdapter(system)
	val mappingUri = new URI(mapping)    
    val resulto=gsn.executeQuery(query,mappingUri)
	
	resulto match{
      case sp:SparqlResults=>sp//sparql(sp)
      case rdf:Model=>rdf//.toString//write(System.out,RDFFormat.TTL)
    }
    
  }

def register(system:String,query:String)={
    val (mapping,uri)=Application.mapGsns(system)
    println("got "+mapping)
    //val props1= new Properties
    //props1++=props
    //props1.put("gsn.endpoint",uri)
    
    val gsn=new EsperAdapter(Global.esper.system)
	val mappingUri = new URI(mapping.toString)    
    val resulto=gsn.registerQuery(query,mappingUri)
	
	resulto 
    
  }


def listen(system:String,query:String,rec:StreamReceiver)={
    val (mapping,uri)=Application.mapGsns(system)
    println("got "+mapping)
    val gsn=new EsperAdapter(Global.esper.system)
	val mappingUri = new URI(mapping.toString)    
    val resulto=gsn.listenToQuery(query,mappingUri,rec)
	
	resulto 
    
  }
  
  def writeModel(model:Model)={
    val out=new ByteArrayOutputStream()
    model.write(out,RDFFormat.TTL)
    new String(out.toByteArray)
  }
 
  /*
 private implicit def binding2String(b:Binding):String=
   b.getName+":" +
     (if (b.getUri!=null) b.getUri else b.getLiteral.getContent)
 
 def sparql(sparql:Sparql)={
   sparql.getResults().getResult().map{r=>
     r.getBinding().map{b=>binding2String(b)}.mkString(",\t")
   }.mkString(",\n")
   /*
    val jax = JAXBContext.newInstance(classOf[Sparql]) ;
 	val m = jax.createMarshaller();
 	val sr = new StringWriter();
 	m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, java.lang.Boolean.TRUE);
 	m.marshal(sparql,sr);
 	sr.toString*/
  }*/

}
