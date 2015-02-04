package controllers
/*
import model.AmazonRating._
import model.{AmazonProduct, AmazonProductAndRating, AmazonRating}
import org.apache.spark.SparkContext
import play.api.mvc._
import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import util.{AmazonPageParser, Recommender}
*/


import java.io.File

import scala.io.Source

import org.apache.log4j.Logger
import org.apache.log4j.Level

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._
import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Application extends Controller {
  
  val Retires = 3
  
/*  val NumRetries = 3
  val MaxRecommendations = 5

  val driver = new MongoDriver
  val connection = driver.connection(List("localhost"))

  val db = connection("amazon_recommendation")
  val ratingCollection = db[BSONCollection]("ratings")

  val RatingFile = "ratings.csv"

  val sc = new SparkContext("local[4]", "recommender")
  sc.addJar("target/scala-2.10/meglytics-visualizer-_2.10-1.0-SNAPSHOT.jar")
  val recommender = new Recommender(sc, RatingFile)

  // return random amazon page and retry multiple times (in case a page is buggy, we try another one)
  private def parseRandomAmazonPageWithRetries(numRetries: Int): Future[AmazonProduct] = {
    val productId = recommender.getRandomProductId
    AmazonPageParser.parse(productId).recoverWith {
      case e: Exception if numRetries >= 0 =>
        parseRandomAmazonPageWithRetries(numRetries - 1)
    }
  }
*/
  
   def main(args: Array[String]) {

    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)

  
    val conf = new SparkConf()
      .setAppName("MovieLensALS")
      .set("spark.executor.memory", "2g")
    val sc = new SparkContext(conf)

   
    val myRatings = getRandomProduct
    val myRatingsRDD = sc.parallelize(myRatings, 1)


    val ProductsListDir = args(0)

    val ratings = sc.textFile(new File(ProductsListDir, "ratings.dat").toString).map { line =>
      val fields = line.split("::")
      // format: (timestamp % 10, Rating(userId, prodId, rating))
      (fields(3).toLong % 10, Rating(fields(0).toInt, fields(1).toInt, fields(2).toDouble))
    }

    val products = sc.textFile(new File(ProductsListDir, "productsList.dat").toString).map { line =>
      val fields = line.split("::")
      // format: (prodId, prodName)
      (fields(0).toInt, fields(1))
    }.collect().toMap

    val recommender = new Recommender(sc, ratings, products)

  /*
  def rating(productIdOpt: Option[String], ratingOpt: Option[Double]) = Action.async {
    val fut = (productIdOpt, ratingOpt) match {
      case (Some(productId), Some(rating)) =>
        ratingCollection.save(AmazonRating("myself", productId, rating))

      case _ => Future.successful()
    }

    fut.flatMap {
      _ => parseRandomAmazonPageWithRetries(NumRetries).map(
        item => Ok(views.html.rating(item))
      ) recover {
        case e: Exception => sys.error(s"Cannot load Amazon page after $NumRetries attempts. Please reload the page")
      }
    }
  }
*/
  def getRandomProduct(Retries: Int): Future[AllRatedProducts] = {  
      
      val ProductID = recommender.getRandomProductID.recoverWith  {
      case e: Exception if Retries >= 0 =>
        GetRandomProduct(Retries - 1)
      }
      ProductID
    }
    
  def index() = Action {
    Redirect("/rating")
  }
 }
   
  def recommendation = Action.async {
    ratingCollection.find(BSONDocument.empty).cursor[AmazonRating].collect[Seq]().flatMap {
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
}
