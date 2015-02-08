package model

import play.api.libs.json.Json
import java.io.File

import scala.io.Source

import org.apache.log4j.Logger
import org.apache.log4j.Level

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._
import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}


case class AllRatedProducts(sc: SparkContext, ratings: RDD[(Long, Rating)]) {
  
  
 /* val ratings = sc.textFile("hdfs://192.168.1.9:8097/ss/retail5.csv").map { line =>
      val fields = line.split(",")
     //   line.split(",")
      (fields(5).toLong % 10, Rating(fields(0).toInt, fields(3).toInt, fields(4).toDouble))
     
    }
  */
  
  private def changeToRating(testTuple: String) = {
    
    val fields = testTuple.split(" ")
   Rating(fields(0).toInt, fields(1).toInt, fields(2).toInt)  
  }
  
  val testTuple = "3,22,4"
  val tple = testTuple.split(",").map(_.trim)
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
   // val candidates = sc.parallelize(products.keys.filter(!aptProductIds.contains(_)).toSeq)
    val candidates = sc.parallelize((aptProductIds).toSeq)
    val recommendations = bestModel.get
      .predict(candidates.map((0, _)))
      .collect()
       .sortBy(- _.rating)
      .take(50)
    
      //return the predictions
    var i = 1
    println("Products matching the given products:")
    recommendations.foreach { r =>
      println("%2d".format(i) + ": " + r.product)
      i += 1
    }
      
      
       sc.stop()
       
       
/** Compute RMSE (Root Mean Squared Error). */
def computeRmse(model: MatrixFactorizationModel, data: RDD[Rating], n: Long): Double = {
val predictions: RDD[Rating] = model.predict(data.map(x => (x.user, x.product)))
val predictionsAndRatings = predictions.map(x => ((x.user, x.product), x.rating))
.join(data.map(x => ((x.user, x.product), x.rating)))
.values
math.sqrt(predictionsAndRatings.map(x => (x._1 - x._2) * (x._1 - x._2)).reduce(_ + _) / n)
}

      
}

