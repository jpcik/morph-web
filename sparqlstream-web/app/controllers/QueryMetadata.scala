package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.json.Json._
import com.hp.hpl.jena.query.QueryFactory
import com.hp.hpl.jena.query.QueryExecutionFactory
import collection.JavaConversions._

object QueryMetadata extends Controller{

  val searchForm=Form(tuple("system"->nonEmptyText,"query"->nonEmptyText))
  
  def maps=Action{
    Ok(views.html.map(QueryExecutor.getProperties,List(),searchForm))
  }

  
  def searchSensors=Action{implicit request =>
    searchForm.bindFromRequest.fold(
        errors =>BadRequest(views.html.index(Sensor.all(),null)),
        vals =>{
          val sensors=QueryExecutor.getSensors(vals._1).toList
          //val ss=Json.toJson(sensors.head)
          val r =QueryExecutor.getProperties
          Ok(views.html.map(r,sensors,searchForm))
        }
        )
  }

}

object QueryExecutor {
  val metadataSparqlUrl="http://localhost:8080/openrdf-workbench/repositories/swissex-mem/query"
  
  def getProperties()={
    val sparql = "PREFIX geo-pos: <http://www.w3.org/2003/01/geo/wgs84_pos#> "+
		"PREFIX foaf: <http://xmlns.com/foaf/0.1/> "+
		"PREFIX swissex: <http://swiss-experiment.ch/metadata#> "+  
		"PREFIX ssn: <http://purl.oclc.org/NET/ssnx/ssn#> "+ 
		"SELECT DISTINCT ?prop "+
		"WHERE { "+
		"?sensor ssn:observes ?propinst." +
		"?propinst a ?prop. " +
		"FILTER isIRI(?prop)."+
		"}";
	val qry = QueryFactory.create(sparql);
	val qrexec = QueryExecutionFactory.sparqlService(metadataSparqlUrl,qry);
	val resp = qrexec.execSelect();

	resp.map{qs=>
	  val propClass=qs.getResource("prop")
	  new ObservedProperty(propClass.getURI,propClass.getLocalName)
	}.toList
  }
  
  def getSensors(obsPropertyUri:String)={
    val sparql = "PREFIX geo-pos: <http://www.w3.org/2003/01/geo/wgs84_pos#> "+
		"PREFIX geo-ont: <http://www.geonames.org/ontology#> "+
		"PREFIX omgeo: <http://www.ontotext.com/owlim/geo#> "+
		"PREFIX foaf: <http://xmlns.com/foaf/0.1/> "+
		"PREFIX dul:	<http://www.loa-cnr.it/ontologies/DUL.owl#> "+
		"PREFIX swissex: <http://swiss-experiment.ch/metadata#> "+  
		"PREFIX ssn: <http://purl.oclc.org/NET/ssnx/ssn#> "+ 
		"PREFIX propPressure: <http://sweet.jpl.nasa.gov/2.1/propPressure.owl#> "+ 
		"PREFIX propTemperature: <http://sweet.jpl.nasa.gov/2.1/propTemperature.owl#> "+ 
		"PREFIX rr: <http://www.w3.org/ns/r2rml#> "+
		"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "+
		"SELECT DISTINCT ?lat ?long ?platform ?sensor " +
		//"?field ?table " +
		"?platformName ?deploymentName ?stime "+
		"WHERE { "+
		//"?tMap 	rr:predicateObjectMap ?obspoMap, ?obppoMap;" +
		//" 		rr:refPredicateObjectMap ?fpoMap; "+
		//"		rr:tableName ?table. "+
		//"?obspoMap rr:objectMap [ rr:object ?sensor ]. "+  
		//"?obppoMap rr:objectMap [ rr:object [ a <"+obsPropertyUri+"> ] ]. "+
		//"?fpoMap rr:refObjectMap   [ rr:parentTriplesMap ?outputTMap ]. "+  		
		//"?outputTMap rr:refPredicateObjectMap [ rr:refObjectMap [ rr:parentTriplesMap ?obsValueTMap]]. "+
		//"?obsValueTMap rr:predicateObjectMap [rr:objectMap [rr:column ?field] ] . "+
		"?sensor ssn:observes [ a <"+obsPropertyUri+"> ]. "+
		"?system ssn:hasSubSystem ?sensor; "+
		"        ssn:onPlatform ?platform; "+
		"        ssn:hasDeployment ?deployment. "+
		"?deployment foaf:name ?deploymentName. "+
		"?platform dul:hasLocation ?region. "+
		"?platform foaf:name ?platformName. " +
		"?region swissex:hasGeometry ?link. "+
		//"?link omgeo:within("+lat1+" "+lon1+" "+lat2+" "+lon2+") . "+
		"?link geo-pos:lat ?lat . "+
		"?link geo-pos:long ?long . "+
		"} ";
    
    println(sparql)
    val qry = QueryFactory.create(sparql)
	val qrexec = QueryExecutionFactory.sparqlService(metadataSparqlUrl,qry)
	val resp = qrexec.execSelect

	resp.map{qs=>
	  val platform=Platform(qs.getLiteral("platformName").getString,
	      qs.getLiteral("lat").getDouble,qs.getLiteral("long").getDouble)
	  val deployment=Deployment(qs.getLiteral("deploymentName").getString)
	  SensorDevice(platform,deployment,Array())
	  //println(qs.getResource("sensor"))
	}
  }
}

case class SensorDevice(platform:Platform,deployment:Deployment,obsProperties:Array[String])
case class Deployment(name:String)
case class Platform(name:String,lat:Double,lon:Double)

case class ObservedProperty(uri:String,label:String)