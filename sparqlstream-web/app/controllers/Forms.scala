package controllers

case class QueryForm (systemid:String,action:String,query:String,
    customMapping:Boolean,showQuery:Boolean,mapping:Option[String])

case class PullForm (systemid:String,action:String,qid:String)
