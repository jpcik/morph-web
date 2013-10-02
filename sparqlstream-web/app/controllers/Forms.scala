package controllers

case class QueryForm (systemid:String,action:String,query:String,
    customMapping:Boolean,mapping:Option[String])

