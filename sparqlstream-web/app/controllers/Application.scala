package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.json.Json._
import org.w3.sparql.results.Sparql
import scala.collection.mutable.ArrayBuffer
import javax.xml.bind.JAXBContext
import java.io.StringWriter
import javax.xml.bind.Marshaller
import es.upm.fi.oeg.morph.stream.gsn.GsnAdapter
import java.net.URI


object Application extends Controller {
  val taskForm=Form("query"->nonEmptyText)
  
  def index = Action {
    Ok(views.html.index(List("Your new application is ready."),taskForm))
  }
 
 def query=Action{implicit request =>
    taskForm.bindFromRequest.fold(
        errors =>BadRequest(views.html.index(Sensor.all(),errors)),
        query=>{
          val r =Sensor.query(query)
          Ok(views.html.result(r,null))
        }
        )
  }

  def posequery=Action{
    Ok(views.html.query(Sensor.all(),taskForm))
  }
 
}

object Sensor{
  val sensors=new ArrayBuffer[String]
  def all():List[String]=sensors.toList
  def query(query:String)={
    val gsn=new GsnAdapter
    
	//val	props = ParameterUtils.load(getClass().getClassLoader().getResourceAsStream("config/config_memoryStore.gsn.properties"))
	val mappingUri = new URI("mappings/citybikes.ttl")    
    val resulto=gsn.executeQuery(query,mappingUri)

    //val trans = new QueryTranslator(props);
	//val s= trans.translate(query, new URI("mappings/citybikes.ttl"));
	//val gQuery = s.asInstanceOf[GsnQuery];
		//gsn.init(props);
		//val exe = new QueryExecutor(props);
		//val sparqlResult:Sparql=exe.query(gQuery,QueryTranslator.getProjectList(query));
	
	sparql(resulto)
  }
 def sparql(sparql:Sparql)={
    val jax = JAXBContext.newInstance(classOf[Sparql]) ;
 	val m = jax.createMarshaller();
 	val sr = new StringWriter();
 	m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, java.lang.Boolean.TRUE);
 	m.marshal(sparql,sr);
 	sr.toString
  }

}
