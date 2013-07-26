package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.json.Json._
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


object Application extends Controller {
  val props= ParameterUtils.load(getClass.getClassLoader.getResourceAsStream("config/siq.properties"))
  val gsns=props.getProperty("gsn.endpoints").split(",")
  val mapGsns=gsns.map(g=>g->("mappings/"+g+".ttl",props.getProperty("gsn.endpoint."+g))).toMap
  val taskForm=Form(tuple("system"->nonEmptyText,"query"->nonEmptyText))
  
  def index = Action {
    Ok(views.html.index(List("Your new application is ready."),taskForm))
  }
 
  
 def query=Action{implicit request =>
    taskForm.bindFromRequest.fold(
        errors =>BadRequest(views.html.index(Sensor.all(),errors)),
        vals =>{
          val r =Sensor.query(vals._1,vals._2)
          Ok(views.html.result(r.asInstanceOf[SparqlResults],mapGsns(vals._1)._2,null))
        }
        )
  }

  def posequery(id:String)=Action{
    //if (!id.equals("citybikes")) BadRequest(views.html.index(null,null))
    Ok(views.html.query(id,Sensor.all(),taskForm))
  }
 
  def posequeryall=Action{
    Ok(views.html.query(null,Sensor.all(),taskForm))
  }
 
}

object Sensor{
  val props= ParameterUtils.load(getClass.getClassLoader.getResourceAsStream("config/siq.properties"))
  val sensors=new ArrayBuffer[String]
  def all():List[String]=sensors.toList
  def query(system:String,query:String)={
    val (mapping,uri)=Application.mapGsns(system)
    println("got "+mapping)
    val props1= new Properties
    props1++=props
    props1.put("gsn.endpoint",uri)
    val gsn=new QueryEvaluator(props1)
    //gsn.props.setProperty("gsn.endpoint","ffffefwfw")
	//val	props = ParameterUtils.load(getClass().getClassLoader().getResourceAsStream("config/config_memoryStore.gsn.properties"))
	val mappingUri = new URI(mapping)    
    val resulto=gsn.executeQuery(query,mappingUri)

    //val trans = new QueryTranslator(props);
	//val s= trans.translate(query, new URI("mappings/citybikes.ttl"));
	//val gQuery = s.asInstanceOf[GsnQuery];
		//gsn.init(props);
		//val exe = new QueryExecutor(props);
		//val sparqlResult:Sparql=exe.query(gQuery,QueryTranslator.getProjectList(query));
	
	resulto match{
      case sp:SparqlResults=>sp//sparql(sp)
      case rdf:Model=>rdf//.toString//write(System.out,RDFFormat.TTL)
    }
    
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
