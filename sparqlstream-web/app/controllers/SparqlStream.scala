package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import es.upm.fi.oeg.siq.tools.ParameterUtils
import es.upm.fi.oeg.siq.sparql.SparqlResults
import com.hp.hpl.jena.query.ResultSetFormatter
import java.io.ByteArrayOutputStream
import com.typesafe.config.ConfigFactory
import es.upm.fi.oeg.morph.stream.evaluate.Mapping
import collection.JavaConversions._
import java.net.URI

object SparqlStream extends Controller {  
  val conf=ConfigFactory.load
  val gsns=conf.getStringList("gsn.systemids")
  val systems=(gsns).map(s=>s->s)
  val mappings=(gsns).map(g=>g->new URI("mappings/"+g+".ttl")).toMap

  def posequery(id:String)=Action { implicit request =>
    val q=request.getQueryString("query")
    println(q)
    //val mapping=Mapping(mappings(id))
    val res=Sensor.query(id,q.get,None).asInstanceOf[SparqlResults]
    val os = new ByteArrayOutputStream()
    ResultSetFormatter.outputAsJSON(os,res.getResultSet)    
    Ok(new String(os.toByteArray))
  }
 

}