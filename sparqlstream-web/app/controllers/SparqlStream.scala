package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.json.Json._
import es.upm.fi.oeg.siq.tools.ParameterUtils
import es.upm.fi.oeg.siq.sparql.SparqlResults
import com.hp.hpl.jena.query.ResultSetFormatter
import java.io.ByteArrayOutputStream

object SparqlStream extends Controller {
  val props= ParameterUtils.load(getClass.getClassLoader.getResourceAsStream("config/siq.properties"))
  val gsns=props.getProperty("gsn.endpoints").split(",")
  val mapGsns=gsns.map(g=>g->("mappings/"+g+".ttl",props.getProperty("gsn.endpoint."+g))).toMap
  
  def posequery(id:String)=Action { request =>
    val q=request.getQueryString("query")
    println(q)
    val res=Sensor.query(id,q.get).asInstanceOf[SparqlResults]
    val os = new ByteArrayOutputStream()
    ResultSetFormatter.outputAsJSON(os,res.getResultSet)    
    Ok(new String(os.toByteArray))
  }
 

}