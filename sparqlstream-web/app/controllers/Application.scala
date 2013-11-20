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
import es.upm.fi.oeg.morph.stream.evaluate.Mapping
import scala.io.Source
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import play.api.libs.Comet
import scala.concurrent._
import scala.util.Random
import akka.actor.ActorRef
import com.hp.hpl.jena.query.ResultSetFormatter
import akka.actor.Kill

object Application extends Controller {
  val conf=ConfigFactory.load
  val users=new collection.mutable.HashMap[String,User] 
  val gsns=conf.getStringList("gsn.systemids")
  val espers=conf.getStringList("esper.systemids")
  val queries=(gsns++espers).map(a=>a->conf.getStringList("morph.streams."+a+".queries").toSeq).toMap
  val allqueries=queries.values.flatten.toSeq
  val systems=(gsns++espers).map(s=>s->s)
  val adapter=(gsns.map{g=>g->new GsnAdapter(g)}++
              espers.map(e=>e->new EsperAdapter(Global.esper.system,e))).toMap
  val mappings=(gsns++espers).map(g=>g->new URI("mappings/"+g+".ttl")).toMap
  val taskForm=Form(mapping("system"->nonEmptyText,"action"->nonEmptyText,"query"->nonEmptyText,
      "customMapping"->boolean, "showQuery"->boolean,"mapping"->optional(text))(QueryForm.apply)(QueryForm.unapply) )
  val pullForm=Form(mapping("system"->nonEmptyText,"action"->nonEmptyText,
      "qid"->nonEmptyText)(PullForm.apply)(PullForm.unapply) )
  val initForm=Form(tuple("system"->nonEmptyText,"query"->nonEmptyText))
  
  def index = Action {implicit request=>
    Ok(views.html.index(List("Your new application is ready."),initForm))
  }
 
  def listen(id:String) = Action {
    val mapping=serializedMapping(id) 
    Ok(views.html.push(id,taskForm.fill(QueryForm(id,"","",false,false,Some(mapping)))))
  }
     
  def query(id:String)=Action{implicit request =>
    taskForm.bindFromRequest.fold(        
      errors =>                
        BadRequest(views.html.query(errors.data("system"),errors)),
      vals =>{
        Logger.debug("getting form "+vals)
        val mapping=if (vals.customMapping) vals.mapping else None
        try {
          val innerquery=if (!vals.showQuery) None else 
            Sensor.rewriteOnly(vals.systemid,vals.query,mapping)
          val r =Sensor.query(vals.systemid,vals.query,mapping)    
          r match{
            case sparqlres:SparqlResults=>
              Ok(views.html.result(sparqlres,null,vals.systemid,innerquery))
            case rdf:Model => 
              Ok(views.html.result(null,rdf,vals.systemid,innerquery))
          }
          
        }
        catch {case e:Exception=>
          BadRequest(views.html.query(vals.systemid,taskForm.fill(vals).withGlobalError(e.getMessage)))
        }
      }
    )
  }

  
  def register(id:String)=Action{implicit request =>
    taskForm.bindFromRequest.fold(        
      errors =>                
        BadRequest(views.html.register(errors.data("system"),errors)),
      vals =>{
        Logger.debug("getting form "+vals)
        val mapping=if (vals.customMapping) vals.mapping else None
        try {
          val innerquery=if (!vals.showQuery) None 
          else Sensor.rewriteOnly(vals.systemid, vals.query, mapping)
          val id =Sensor.register(vals.systemid,vals.query,mapping)  
          regQuery(id, request)
          Ok(views.html.qid(id,pullForm.fill(PullForm(vals.systemid,"pull",id)),null,null,innerquery))            
        }
        catch {case e:Exception=>
          BadRequest(views.html.register(vals.systemid,taskForm.fill(vals).withGlobalError(e.getMessage)))
        }
      }
    )
  }

  
  private def remQuery(qid:String,request:RequestHeader)={
    val ip=request.remoteAddress
    val qids=users.getOrElse(ip, User(ip,Seq())).qids
    users.update(ip, User(ip, qids.filterNot(_==qid)))
  }
  
  private def regQuery(qid:String,request:RequestHeader)={
    val ip=request.remoteAddress
    val qids=users.getOrElse(ip, User(ip,Seq())).qids
    users.update(ip, User(ip, qids++Seq(qid)))
  }

  private def serializedMapping(systemid:String)={    
    val mappingStream=getClass.getClassLoader.getResourceAsStream(mappings(systemid).toString)    
    Source.fromInputStream(mappingStream).getLines.mkString("\n")
  }
  
  def posequery(id:String)=Action{ implicit request =>
    val mapping=serializedMapping(id) 
    Ok(views.html.query(id,taskForm.fill(QueryForm(id,"","",false,false,Some(mapping)))))    
  }

  def registerquery(id:String)=Action{ implicit request =>
    val mapping=serializedMapping(id) 
    Ok(views.html.register(id,taskForm.fill(QueryForm(id,"","",false,false,Some(mapping)))))    
  }
    
  def posequeryall=Action{ implicit request =>   
    Ok(views.html.query("",taskForm.fill(QueryForm("","","",false,false,None))))
  }
 
  def pull=Action{implicit request =>
    pullForm.bindFromRequest.fold(        
      errors =>                
        BadRequest(views.html.qid(errors.data("system"),errors,null,null,None)),
      vals =>{
        Logger.debug("getting form "+vals)
        if (vals.action.equals("pull")){
          try {
          val r =Sensor.pull(vals.systemid, vals.qid)
          Ok(views.html.qid(vals.systemid,
              pullForm.fill(PullForm(vals.systemid,vals.action,vals.qid)),r,null,None))
          } catch {case e:Exception=>
            BadRequest(views.html.qid(vals.systemid,pullForm.fill(vals).withGlobalError(e.getMessage),null,null,None))}
        }
        else {
          Sensor.remove(vals.systemid,vals.qid)
          remQuery(vals.qid, request)
          val mapping=serializedMapping(vals.systemid)
          Ok(views.html.register(vals.systemid,
              taskForm.fill(QueryForm(vals.systemid,"","",false,false,Some(mapping)))))   
        }
      }
    )
  }

  
  val f=Promise[Option[String]]()
  
  lazy val clock: Enumerator[String] = {    
    import java.util._
    import java.text._
    
    val dateFormat = new SimpleDateFormat("HH mm ss")    
    Enumerator.generateM {
      f.future
      //Promise.timeout(Some(dateFormat.format(new Date)), 1000 milliseconds)
    }
  }  
  def tripp =  Action {
    val events = clock//Enumerator("kiki", "foo", "bar")
    Ok.stream(events &> Comet(callback = "console.log"))
    //Ok.stream(Enumerator("kiki", "foo", "bar").andThen(Enumerator.eof))
  }
  
  //Test websocket only
  def push = WebSocket.async[String] { request =>try{
    val query=request.queryString("query").head
    Logger.debug("Web socket got: "+query) 
    Logger.debug("Now starting query listener")

    val dd=Akka.system.actorOf(Props(new Drop))

    val rec = new ResultsReceiver(dd)
    val qid=try Sensor.listen("social", query, rec)
    catch {case e:Exception=> throw new Exception("could not create: "+e)} 
    
    val in = Iteratee.foreach[String]{s=>
      println("received meanwhile: "+s) 
    }.mapDone { _ =>
      println("Disconnected")
      dd ! Stop()
      dd ! PoisonPill
    }

     implicit val timeout = Timeout(5 second)
      (dd ? Subs(qid)).map{
       case Accpt(e)=>(in,e)
     }
  }
  catch {
    case e:Exception=>           
        future {(Iteratee.foreach[String]{println},
        Enumerator("Fatal Error, please disconnect and try again.","Error: "+e.getMessage).andThen(Enumerator.eof))}
    case a:Throwable=>throw a 
  }
     // (in,Await.result(out,timeout.duration))
  }

  def toJson(res:SparqlResults)={
    val os = new ByteArrayOutputStream
    ResultSetFormatter.outputAsJSON(os,res.getResultSet)    
    new String(os.toByteArray)
  }
}

case class Accpt(e:Enumerator[String]) 
case class Msg(m:String)
case class Subs(qid:String)
case class Stop()

class Drop extends Actor{
  var queryId:String=_ 
  def stopQuery=
    if (queryId!=null) Sensor.remove("social", queryId)
  
  val (enum,channel)=Concurrent.broadcast[String]
  def receive={
    case Subs(qid)=>
      queryId=qid
      sender ! Accpt(enum)
    case Msg(m)=>broadcast(m)
    case Stop()=>stopQuery
    case p=> println("mjmjm"+p)
  }
  
  def broadcast(m:String)=channel.push(m)
}

object Sensor{
  val sensors=new ArrayBuffer[String]
  def all():List[String]=sensors.toList
  def query(system:String,query:String,mappingStr:Option[String])={
    val mapping=
      if (mappingStr.isDefined) Mapping(mappingStr.get)
      else Mapping(Application.mappings(system))
    val adapter=Application.adapter(system)
    val resulto= adapter.executeQuery(query,mapping)
	resulto match{
      case sp:SparqlResults=>sp//sparql(sp)
      case rdf:Model=>rdf//.toString//write(System.out,RDFFormat.TTL)
    }
    
  }

  def remove(system:String,qid:String)= {
    Application.adapter(system).removeQuery(qid)    
  }
  
  def register(system:String,query:String,mappingStr:Option[String])={
    val mapping=
      if (mappingStr.isDefined) Mapping(mappingStr.get)
      else Mapping(Application.mappings(system))
    val adapter=Application.adapter(system)	    
    val resulto=adapter.registerQuery(query,mapping)
    //catch {case e:Exception=>throw new IllegalArgumentException("Query could not be registered.",e)}
	resulto     
  }

  def pull(system:String,qid:String)={
    Application.adapter(system).pull(qid)
  }

  def listen(system:String,query:String,rec:StreamReceiver)={
    val mappingUri=Application.mappings(system)    
    val gsn=Application.adapter(system)
    val resulto=gsn.listenToQuery(query,Mapping(mappingUri),rec)	    
	resulto     
  }
  
  def rewriteOnly(system:String,query:String,mappingStr:Option[String])={
    val mapping=
      if (mappingStr.isDefined) Mapping(mappingStr.get)
      else Mapping(Application.mappings(system))
    Application.adapter(system).rewriteSerialize(query, mapping)
  }
  
  def writeModel(model:Model)={
    val out=new ByteArrayOutputStream()
    model.write(out,RDFFormat.TTL)
    new String(out.toByteArray)
  }
 

}

case class User(ip:String,qids:Seq[String])

//class ResultsReceiver(pp:Promise[Option[String]]) extends StreamReceiver{   
class ResultsReceiver(actor:ActorRef) extends StreamReceiver{
  override def receiveData(s:SparqlResults):Unit={
    val res=Application.toJson(s)
    Logger.debug("got: "+res)
    actor ! Msg(res)    
  }
}