#!/usr/env/amm

import $ivy.`io.circe::circe-core:0.4.1`
import $ivy.`io.circe::circe-generic:0.4.1`
import $ivy.`io.circe::circe-parser:0.4.1`

import scala.language.dynamics

import io.circe._
import io.circe.generic.auto._
import io.circe.jawn._
import io.circe.syntax._
import cats.data.Xor

def get(json: Json, field: String): Json = json.asObject.get.apply(field).get
def get(json: Json, idx: Int): Json = json.asArray.get.apply(idx)
def array(json: Json): Seq[String] = json.asArray.get.map(_.as[String].getOrElse(""))
def fields(json: Json): Seq[String] = json.asObject.get.fields

val json = read('dirJson / "67b12cb.json")
val doc = parse(json).getOrElse(Json.Null)

val apps = get(doc, "apps")
val names = fields(apps)

for(name <- names){
  val app = get(apps, name)
  val runs = get(app, "runs")
  val configs = array(get(app, "configs"))
  for(config <- configs){
    val run = get(runs, config)
    val time = get(run, "avg_time")
    println(s"Time for $name $config: $time")
  }
}



