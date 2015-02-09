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

case class RecommendProduct(productId: Int, nooforders: Double) {
  val json = "{\"productId\":\"" + productId + "\",\"nooforders\":" + nooforders + "}"
}
//case class AllRatedProducts(rating: String) {

object Retail {

  val sc = new SparkContext(MConfig.sparkurl, "recommender")
  sc.addJar("target/scala-2.10/meglyticsvisualizer_2.10-1.0-SNAPSHOT.jar")

  def changeToRating(testTuple: String) = {
      println("==============================="+ testTuple)
      val fields = testTuple.split(" ")
      Rating(fields(0).toInt, fields(1).toInt, fields(2).toInt)
    }
  
  // private def buyingbehaviour(product_name: String): ValidationNel[Throwable, RecommendProducts]= {
  def buyingbehaviour(product_name: String, filename: String): List[String] = {
    val ratings = sc.textFile(MConfig.sparkurl + filename).map { line =>
      val fields = line.split(",")
      //   line.split(",")
      (fields(5).toLong % 10, Rating(fields(0).toInt, fields(3).toInt, fields(4).toDouble))

    }
    println("ratings==========================> " + ratings)    

    val testTuple = "3459,9850,5" //take a simple value
    val tple = testTuple.split(",").map(_.trim)
    println("---------------------==========="+ tple)
    val Tuple = tple.map(changeToRating)     

    val myRatingsRDD = sc.parallelize(Tuple)

    val numRatings = ratings.count()

    val numPartitions = 4
    val training = ratings.filter(x => x._1 < 6)
      .values
      .union(myRatingsRDD)
      .repartition(numPartitions)
      .cache()
    val validation = ratings.filter(x => x._1 >= 6 && x._1 < 8)
      .values
      .repartition(numPartitions)
      .cache()
    val test = ratings.filter(x => x._1 >= 8).values.cache()

    val numTraining = training.count()
    val numValidation = validation.count()
    val numTest = test.count()

    println("Training: " + numTraining + ", validation: " + numValidation + ", test: " + numTest)

    val ranks = List(8, 12)
    val lambdas = List(0.1, 10.0)
    val numIters = List(10, 20)

    var bestModel: Option[MatrixFactorizationModel] = None
    var bestValidationRmse = Double.MaxValue

    var bestRank = 0
    var bestLambda = -1.0
    var bestNumIter = -1

    for (rank <- ranks; lambda <- lambdas; numIter <- numIters) {
      val model = ALS.train(training, rank, numIter, lambda)
      val validationRmse = computeRmse(model, validation, numValidation)

      //rmse validation done, now deriving the bestModel

      if (validationRmse < bestValidationRmse) {
        bestModel = Some(model)
        bestValidationRmse = validationRmse
        bestRank = rank
        bestLambda = lambda
        bestNumIter = numIter
      }
    }

    val testRmse = computeRmse(bestModel.get, test, numTest) //just a simple test, 
    println(testRmse) //compare this with normal rmse equation, it is a lot better. 

    val aptProductIds = Tuple.map(_.product).toSet
    //val candidates = sc.parallelize(products.keys.filter(!aptProductIds.contains(_)).toSeq)
    val candidates = sc.parallelize((aptProductIds).toSeq)
    val recommendations = bestModel.get
      .predict(candidates.map((0, _)))
      .collect()
      .sortBy(-_.rating)
      .take(50)

    //return the predictions

    println("Products matching the given products:")

    val finalList = ListBuffer[String]()
    recommendations.foreach { r =>
      val ProductName = r.product
      val nooforders = r.rating
      val finalJson = new RecommendProduct(ProductName, nooforders).json
      finalList += finalJson
    }
    println("----------------------------------" + finalList.toList)
    sc.stop()
    finalList.toList
  }

  def customersegmentation(product_name: String, filename: String) = {

  }

  def profitability(product_name: String, filename: String) = {

  }

  /** Compute RMSE (Root Mean Squared Error). */
  def computeRmse(model: MatrixFactorizationModel, data: RDD[Rating], n: Long): Double = {
    val predictions: RDD[Rating] = model.predict(data.map(x => (x.user, x.product)))
    val predictionsAndRatings = predictions.map(x => ((x.user, x.product), x.rating))
      .join(data.map(x => ((x.user, x.product), x.rating)))
      .values
    math.sqrt(predictionsAndRatings.map(x => (x._1 - x._2) * (x._1 - x._2)).reduce(_ + _) / n)
  }

}



