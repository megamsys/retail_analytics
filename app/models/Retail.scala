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

import com.esotericsoftware.kryo.Kryo
import org.apache.spark.serializer.KryoRegistrator

import scala.collection.mutable.ListBuffer

case class RecommendProduct(productId: Int, nooforders: Double) {
  val json = "{\"productId\":\"" + productId + "\",\"nooforders\":" + nooforders + "}"
}
case class AllRatedProducts() {

//object Retail {
   println("Now creating a spark instance--------------")
    val conf = new SparkConf().setMaster(MConfig.sparkurl).setAppName("Meglytics Viz")
    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    conf.set("spark.kryo.registrator", "models.MyRegistrator")
  val sc = new SparkContext(conf)
  sc.addJar("target/scala-2.10/meglyticsvisualizer_2.10-1.0-SNAPSHOT.jar")
  
  println("DONE---")
  
  def changeToRating(Suser: Int, prodId: Int, MaxR: Double): Rating = {
      println("==============================="+ Suser + prodId + MaxR)
     val rat = Rating(Suser.toInt, prodId.toInt, MaxR.toDouble)
     println(rat)
      rat
      }
  
  // private def buyingbehaviour(product_name: String): ValidationNel[Throwable, RecommendProducts]= {
  def buyingbehaviour(product_id: Int, filename: String): List[String] = {
    
    println(" talking to hadoop! hold thy..")
    
    val ratings = sc.textFile("hdfs://192.168.1.9:8097/analytics/" + filename).map { line =>
      val fields = line.split(",")
      //   line.split(",")
       (fields(3).toLong % 10, Rating(fields(0).toInt, fields(1).toInt, fields(6).toDouble))

    }
    
    val products = sc.textFile("hdfs://192.168.1.9:8097/analytics/products.csv").map { line =>
      val fields = line.split(",")
      //   line.split(",")
      (fields(0).toInt, fields(1))

    }.collect().toMap
    println("ratings==========================> " + ratings)    
    val Suser = 3344
   val prodId = product_id
   val MaxR = 5
    val Tuple = changeToRating(Suser, prodId, MaxR)     

    
    val myRatingsRDD = sc.parallelize(Seq(Tuple))

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
    var model[] = ""  
    for (rank <- ranks; lambda <- lambdas; numIter <- numIters) {
       model = ALS.train(training, rank, numIter, lambda) }
    /*  println("First RMSE test")
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
   println("Now running rmse---")
    val testRmse = computeRmse(bestModel.get, test, numTest) //just a simple test, 
    println(testRmse)
    println("PRINTING RMSE NOW ---")//compare this with normal rmse equation, it is a lot better. 
*/
    val aptProductIds = Seq(Tuple).map(_.product).toSet
    val candidates = sc.parallelize(products.keys.filter(!aptProductIds.contains(_)).toSeq)
    //val candidates = sc.parallelize((aptProductIds).toSeq)
 /*   val recommendations = bestModel.get
      .predict(candidates.map((0, _)))
      .collect()
      .sortBy(-_.rating)
      .take(50)
   */   
      val recommendations = model.predict(candidates.map((0,_)))
                            .collect()
                            .sortBy(-_.rating)
                            .take(5)

    //return the predictions

    println("Products matching the given products:")

    val finalList = ListBuffer[String]()
    recommendations.foreach { r =>
      val ProductName = r.product
      val nooforders = r.rating
      println(ProductName)
      println("GOt-IT----------------")
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
 /* def computeRmse(model: MatrixFactorizationModel, data: RDD[Rating], n: Long): Double = {
    println("Entering computeRmse------")
    val predictions: RDD[Rating] = model.predict(data.map(x => (x.user, x.product)))
    println(predictions)
    val predictionsAndRatings = predictions.map(x => ((x.user, x.product), x.rating))
      .join(data.map(x => ((x.user, x.product), x.rating)))
      .values
      println(predictionsAndRatings)
    math.sqrt(predictionsAndRatings.map(x => (x._1 - x._2) * (x._1 - x._2)).reduce(_ + _) / n)
  }
  */
  

}

class MyRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    //kryo.setRegistrationRequired(false)
    kryo.register(classOf[AllRatedProducts])
  }


}
