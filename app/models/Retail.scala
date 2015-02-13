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
//case class AllRatedProducts() {

object Retail {
   println("Now creating a spark instance--------------")
    val conf = new SparkConf().setMaster(MConfig.sparkurl).setAppName("Meglytics Viz")
    
  val sc = new SparkContext(conf)
  sc.addJar("target/scala-2.10/meglyticsvisualizer_2.10-1.0-SNAPSHOT.jar")
  
  println("DONE---")
  
 
  
  // private def buyingbehaviour(product_name: String): ValidationNel[Throwable, RecommendProducts]= {
  def buyingbehaviour(product_id: Int, filename: String): List[String] = {
    
    println(" talking to hadoop! hold thy..")
    
    val ratings = sc.textFile("hdfs://192.168.1.9:8097/user/megamwork/" + filename).map { line =>
      val fields = line.split("::")
      //   line.split(",")
     
       (fields(3).toLong % 10, Rating(fields(0).toInt, fields(1).toInt, fields(2).toDouble))

    }
    
    val products = sc.textFile("hdfs://192.168.1.9:8097/user/megamwork/movies.dat").map { line =>
      val fields = line.split("::")
      //   line.split(",")
      (fields(0).toInt, fields(1))

    }.collect().toMap
    println("ratings==========================> " + ratings.count()) 
    ratings.count
    
   /* 
    val userId = 1
    val prodId = product_id
    val maxRating = 5
        
    val returnedRating = Rating(userId, prodId, maxRating.toDouble)
    
    println("returnedRating========>" + returnedRating)
    val myRatingsRDD = sc.parallelize(Seq(returnedRating))
    println("myRatingsRDD=======================>" + myRatingsRDD.count())
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
    println("Training========>" + training + "validation======>" + validation + "test=====>" + test)
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
      println("First RMSE test")
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
 
    val aptProductIds = Seq(returnedRating).map(_.product).toSet
    val candidates = sc.parallelize(products.keys.filter(!aptProductIds.contains(_)).toSeq)
    //val candidates = sc.parallelize((aptProductIds).toSeq)
    val recommendations = bestModel.get
      .predict(candidates.map((0, _)))
      .sortBy(-_.rating)
      .take(50)
    
   /*  val aptProductIds = Seq(returnedRating).map(_.product).toSet
     println("aptProductsIds==========>" + aptProductIds)
     val candidates = sc.parallelize(products.keys.filter(!aptProductIds.contains(_))toSeq)
     //val candidates = sc.parallelize((0 until 50).filterNot(aptProductIds.contains))
    println("candidates printing=============>" + candidates.count())
     val recommendations = model.predict(candidates.map((0,_)))
                             //.collect()
                            //.sortBy(-_.rating)
                            //.take(5)
    println("-----------------")
   */
     println("recommendations======>" + recommendations)
    //return the predictions

    val finalList = ListBuffer[String]()
 /*  recommendations.foreach ({ r =>
      val ProductName = r.product
      val nooforders = r.rating
      println(ProductName)
      println("GOt-IT----------------")
      val finalJson = new RecommendProduct(ProductName, nooforders).json
      finalList += finalJson
   }) */
    
     var i = 1
    println(" recommended for you:")
    recommendations.foreach { r =>
      println("%2d".format(i) + ": " + products(r.product))
      i += 1
    }
    
    
   //recommendations.collect().foreach(line => println(line))
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
    println("Entering computeRmse------")
    val predictions: RDD[Rating] = model.predict(data.map(x => (x.user, x.product)))
    println(predictions)
    val predictionsAndRatings = predictions.map(x => ((x.user, x.product), x.rating))
      .join(data.map(x => ((x.user, x.product), x.rating)))
      .values
      println(predictionsAndRatings)
    math.sqrt(predictionsAndRatings.map(x => (x._1 - x._2) * (x._1 - x._2)).reduce(_ + _) / n)
  }
  
}
*/
    
    val numIterations = 20
val model = ALS.train(ratings, 1, 20, 0.01)


