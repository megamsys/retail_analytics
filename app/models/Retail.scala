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

import play.api.libs.json.Json
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

import scala.collection.mutable.ListBuffer

case class RecommendProduct(productId: String, nooforders: Double) {
  val json = "{\"productId\":\"" + productId + "\",\"nooforders\":" + nooforders + "}"
}
//case class AllRatedProducts() {

object Retail {
  println("Now creating a spark instance--------------")
  val conf = new SparkConf().setMaster(MConfig.sparkurl).setAppName("Meglytics Viz")

  val sc = new SparkContext(conf)
  sc.addJar("target/scala-2.10/meglyticsvisualizer_2.10-1.0-SNAPSHOT.jar")

  println("DONE---")

  // private def buyingbehaviour(product_name: String): ValidationNel[Throwable, RecommendProducts]= {

  def buyingbehaviour(product_id: Int, filename: String): List[String] = {

    println("Talking to hadoop, hold thy...")
    val rawData = sc.textFile("hdfs://192.168.1.9:8097/user/megamwork/" + filename)
    val ratings = rawData.map(_.split(',') match {
      case Array(user, item, rate) =>
        Rating(user.toInt, item.toInt, rate.toDouble)
    })

    val products = sc.textFile("hdfs://192.168.1.9:8097/user/megamwork/productsnew1.csv").map { line =>
      val fields = line.split(',')
      (fields(0).toInt, fields(1))
    }.collect().toMap

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
    predRating.collect().sortBy(-_.rating).take(5).foreach { r =>
      println("%2d".format(i) + ": " + products(r.product))
      val productId = r.product
      val nooforders = r.rating
      val ProductName = products(productId)
      val finalJson = new RecommendProduct(ProductName, nooforders).json
      finalList += finalJson
      i += 1
    }

    println(finalList.toList)
    sc.stop()

    finalList.toList
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

    



