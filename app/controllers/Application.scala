package controllers

import play.api.mvc._
import java.io.File
import scala.io.Source
import org.apache.log4j.Logger
import org.apache.log4j.Level
import play.api.mvc._
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._
import org.apache.spark.mllib.recommendation.{ ALS, Rating, MatrixFactorizationModel }
//import util.Recommender
import model.AllRatedProducts

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Application extends Controller {

  val sc = new SparkContext("spark://192.168.1.9:7077", "recommender")
  //val sc = new SparkContext("spark://168.235.145.68:7077", "recommender")
  sc.addJar("target/scala-2.10/meglyticsvisualizer_2.10-1.0-SNAPSHOT.jar")

 /* def index() = Action {
    // Redirect("/ratings")
    // val ratings = sc.textFile("hdfs://168.235.145.68:8097/ss/retail5.csv").map { line =>
    val ratings = sc.textFile("hdfs://192.168.1.9:8097/ss/retail5.csv").map { line =>
      val fields = line.split(",")
      //   line.split(",")
      (fields(5).toLong % 10, Rating(fields(0).toInt, fields(3).toInt, fields(4).toDouble))

    }
    // val ratings = sc.textFile("hdfs://localhost:8097/ss/retail1.csv")
    // val counts = ratings.flatMap(line => line.split(" "))
    //              .map(word => (word, 1))
    //              .reduceByKey(_ + _)

    val numRatings = ratings.count()

    println("Got " + numRatings)

    println("Ratings:" + ratings)
    // println("counts:" + counts.count())
    sc.stop()
    Ok(views.html.ratings("hello"))
  }*/
  
  def index() = Action {
    Ok(views.html.index("Megam Analytics."))
  }

  def ratings() = Action { request =>
    //  val ratings_initial = sc.textFile("hdfs://localhost:8097/analytics/us3.csv").map { line =>
    val ratings = sc.textFile("retail.csv").map { line =>
        val fields = line.split(",")
      //line.split(",")
       (fields(6).toLong % 10, Rating(fields(0).toInt, fields(1).toInt, fields(2).toInt))

    }
    
    val recommend = new AllRatedProducts(sc, ratings)
    val numRatings = ratings.count()

    println("Got " + numRatings)

    println("Ratings:" + ratings)

    Ok(views.html.ratings("hello"))

  }

}
