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


//case class Product(itemId: String, title: String, url: String, img: String, description: String)
case class AllRatedProducts(sc: SparkContext, rating: String) {
  
  
 /* val ratings = sc.textFile("hdfs://192.168.1.9:8097/ss/retail5.csv").map { line =>
      val fields = line.split(",")
     //   line.split(",")
      (fields(5).toLong % 10, Rating(fields(0).toInt, fields(3).toInt, fields(4).toDouble))
     
    }
  
  val testTuple = "TV" //enter a test data
  val myRatingsRDD = sc.parallelize(testTuple, 1)
  
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
         
      
    val aptProductIds = myRatings.map(_.product).toSet
    val candidates = sc.parallelize(products.keys.filter(!aptProductIds.contains(_)).toSeq)
    val recommendations = bestModel.get
      .predict(candidates.map((0, _)))
      .collect()
      .sortBy(- _.rating)
      .take(50)
    
      //return the predictions
      var i = 1
    println("Products matching the given products:")
    recommendations.foreach { r =>
      println("%2d".format(i) + ": " + products(r.product))
      i += 1
    }
      
*/
      
      
      
}

