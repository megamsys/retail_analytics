package controllers

import java.io.File

import scala.io.Source

import org.apache.log4j.Logger
import org.apache.log4j.Level
import play.api.mvc._
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._
import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}
import util.Recommender
import model.AllRatedProducts._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Application extends Controller {
  
  val Retires = 3
  
   def main(args: Array[String]) {

    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)

  
    val conf = new SparkConf()
      .setAppName("Meglytics Visualizer")
      .set("spark.executor.memory", "2g")
    val sc = new SparkContext(conf)

   
    val myRatings = getRandomProduct
    //val myRatingsRDD = sc.parallelize(myRatings, 1)


    val ProductsListDir = args(0)
    
    val ratings = ProductsListDir + "ratings.dat"

    //val ratings = sc.textFile(new File(ProductsListDir, "ratings.dat").toString).map { line =>
      //val fields = line.split("::")
      // format: (timestamp % 10, Rating(userId, prodId, rating))
      //(fields(3).toLong % 10, Rating(fields(0).toInt, fields(1).toInt, fields(2).toDouble))
   // }

    val products = sc.textFile(new File(ProductsListDir, "productsList.dat").toString).map { line =>
      val fields = line.split("::")
      // format: (prodId, prodName)
      (fields(0).toInt, fields(1))
    }.collect().toMap

    val recommender = new Recommender(sc, ratings, products, myRatings)


  def getRandomProduct(Retries: Int) = recommender.getRandomProductID
  
 // def getRandomProduct(Retries: Int): Future[AllRatedProducts] = recommender.getRandomProductID
 //to add model later   
  def index() = Action {
    Redirect("/rating")
  }
 }
 /*  
  def recommendation = Action.async {
    ratingCollection.find(BSONDocument.empty).cursor[Rating].collect[Seq]().flatMap {
      ratings =>
        val finalProducts = recommender.predict(ratings.take(MaxRecommendations)).toSeq
        val productsFut = Future.traverse(amazonRatings) (
          finalProducts => {
            println("Similar Products:" + finalProducts)
            // remove errored product pages
            AmazonPageParser.parse(amazonRating.productId).map(Option(_)).recover {case _: Exception => None}
          }
        )
        productsFut.map {
          products => Ok(views.html.pieAndArea(products.flatten))
        }
    }
  }
  * 
  */
}
