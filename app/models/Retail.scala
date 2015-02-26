/* 
** Copyright [2015-2016] [Megam Systems]
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
** http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/
package models

import scalaz._
import Scalaz._
//import scalaz.effect.IO
import scalaz.EitherT._
import scalaz.Validation
//import scalaz.Validation.FlatMap._
import scalaz.NonEmptyList._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
//import play.api.libs.json.Json
import java.io.File
import scala.io.Source
import com.typesafe.config._
import org.apache.log4j.Logger
import org.apache.log4j.Level
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._
import org.apache.spark.mllib.recommendation.{ ALS, Rating, MatrixFactorizationModel }
import models.stack._
import java.nio.charset.Charset

import scala.collection.mutable.ListBuffer

case class RetailRecommand(productId: String, nooforders: String) {
  val json = "{\"productId\":\"" + productId + "\",\"nooforders\":" + nooforders + "}"
  
  def toJValue: JValue = {
    import net.liftweb.json.scalaz.JsonScalaz.toJSON
    import models.json.RetailRecommandSerialization.{ writer => RetailRecommandWriter }
    toJSON(this)(RetailRecommandWriter)
  }

  def toJson(prettyPrint: Boolean = false): String = if (prettyPrint) {
    pretty(render(toJValue))
  } else {
    compactRender(toJValue)
  }
  
}

object RetailRecommand {
  def empty: RetailRecommand = new RetailRecommand(new String(), new String())

  def fromJValue(jValue: JValue)(implicit charset: Charset = Charset.forName("UTF-8")): Result[RetailRecommand] = {
    import net.liftweb.json.scalaz.JsonScalaz.fromJSON
    import models.json.RetailRecommandSerialization.{ reader => RetailRecommandReader }
    fromJSON(jValue)(RetailRecommandReader)
  }

 /* def fromJson(json: String): Result[RetailRecommand] = (Validation.fromTryCatch[net.liftweb.json.JValue] {
    play.api.Logger.debug(("%-20s -->[%s]").format("---json------------------->", json))
    parse(json)
  } leftMap { t: Throwable =>
    UncategorizedError(t.getClass.getCanonicalName, t.getMessage, List())
  }).toValidationNel.flatMap { j: JValue => fromJValue(j) }
*/
}

//case class AllRatedProducts() {

object Retail {
 // println("Now creating a spark instance--------------")
 // val conf = new SparkConf().setMaster(MConfig.sparkurl).setAppName("Meglytics Viz")

 // val sc = new SparkContext(conf)
 // sc.addJar("target/scala-2.10/meglyticsvisualizer_2.10-1.0-SNAPSHOT.jar")

 // println("DONE---")

  // private def buyingbehaviour(product_name: String): ValidationNel[Throwable, RecommendProducts]= {

  def buyingbehaviour(product_id: Int, filename: String): Tuple2[RetailRecommands, String] = {

    println("Now creating a spark instance--------------")
  val conf = new SparkConf().setMaster(MConfig.sparkurl).setAppName("Meglytics Viz")

  val sc = new SparkContext(conf)
  sc.addJar("target/scala-2.10/meglyticsvisualizer_2.10-1.0-SNAPSHOT.jar")

  println("DONE---")
    
    println("Talking to hadoop, hold thy...")
    
    var productname = ""
    
    val rawData = sc.textFile(MConfig.hdfsuri+"user/" + MConfig.username + "/" + filename)
    val ratings = rawData.map(_.split(',') match {
      case Array(user, item, rate) =>
        Rating(user.toInt, item.toInt, rate.toDouble)
    })

    val products = sc.textFile(MConfig.hdfsuri+"user/" + MConfig.username + "/" + MConfig.productsfile).map { line =>
      val fields = line.split(',')     
      (fields(0).toInt, fields(1))
    }.collect().toMap
    
    /* sc.textFile(MConfig.hdfsuri+"user/" + MConfig.username + "/" + MConfig.productsfile).foreach { line =>
      val fields = line.split(',')
      println("--------------------------")
      println(fields(0).toInt)
      println(product_id)
      println("--------------------------")
      if(fields(0).toInt == product_id) {
        productname = fields(1)
        println("++++++++++++++++++++++++++++++++++++")
        println(productname)
      }     
    }*/
    
    productname = products(product_id)   
    val userId = 1
    val prodId = product_id
    val maxRating = 5

    val returnedRating = Rating(userId, prodId, maxRating.toDouble)

    println("Done talking to hadoop!..\n\n\nLets crunch some data to get insight!!")

    val myRatingsRDD = sc.parallelize(Seq(returnedRating))
    println("myRatingsRDD=======================>" + myRatingsRDD.count())
    val numRatings = ratings.count()

    val training = ratings.union(myRatingsRDD)
    val rank = 1
    val numIterations = 5

    println("Training!....")
    val model = ALS.train(training, rank, numIterations, 0.01)
   
    val usersProducts = ratings.map {
      case Rating(user, product, rate) =>
        (user, product)
    }
    println("Predicting now!!")
    val predictions = model.predict(usersProducts).map {
      case Rating(user, product, rate) =>
        ((user, product), rate)
    }

    println("Yaay! done predicting")

    val ratesAndPreds = ratings.map {
      case Rating(user, product, rate) =>
        ((user, product), rate)
    }.join(predictions)
    val MSE = ratesAndPreds.map {
      case ((user, product), (r1, r2)) =>
        val err = (r1 - r2)
        err * err
    }.mean()

    val predRating = predictions.map {
      case ((user, product), rate) =>
        Rating(user, product, rate)
    }

    predRating.collect().sortBy(-_.rating).take(5).foreach(println)

    var i = 1
    val finalList = ListBuffer[String]()
    val finalList1 = ListBuffer[RetailRecommand]()
    predRating.collect().sortBy(-_.rating).take(5).foreach { r =>
    //  println("%2d".format(i) + ": " + products(r.product))
      val productId = r.product
      val nooforders = "%.1f".format(r.rating)     
      val ProductName = products(productId)
      val finalJson = new RetailRecommand(ProductName, nooforders).json
      val finalJson1 = new RetailRecommand(ProductName, nooforders)
      finalList += finalJson
      finalList1 += finalJson1
      i += 1
    } 
    
    val rc = RetailRecommands(finalList1.toList) 
    //sc.stop()   
    val out = Tuple2(rc, productname)
    sc.stop()
    out
  }
  
  def toJValue(nres: RetailRecommands): JValue = {

      import net.liftweb.json.scalaz.JsonScalaz.toJSON
      import models.json.RetailRecommandsSerialization.{ writer => RetailRecommandsWriter }
      toJSON(nres)(RetailRecommandsWriter)
    }

  def customersegmentation(product_name: String, filename: String) = {

  }

  def profitability(product_name: String, filename: String) = {

  }

  /** Compute RMSE (Root Mean Squared Error). */
  def computeRmse(model: MatrixFactorizationModel, data: RDD[Rating], n: Long): Double = {
    println("Entering computeRmse------")
    val predictions: RDD[Rating] = model.predict(data.map(x => (x.user, x.product)))
    println(predictions)
    val predictionsAndRatings = predictions.map(x => ((x.user, x.product), x.rating))
      .join(data.map(x => ((x.user, x.product), x.rating)))
      .values
    println(predictionsAndRatings)
    math.sqrt(predictionsAndRatings.map(x => (x._1 - x._2) * (x._1 - x._2)).reduce(_ + _) / n)
  }

} //obj closing here

    



