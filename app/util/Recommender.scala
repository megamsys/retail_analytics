package util

import org.jblas.DoubleMatrix
import org.apache.spark.rdd._
import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}
import scala.util.Random

import org.apache.spark.SparkContext
import org.joda.time.{Seconds, DateTime}

class Recommender(@transient sc: SparkContext, ratings: RDD[Unit], products: Map[Int, String]) extends Serializable {
 
 /* println(ratings)
  val ratings_parse  = sc.textFile(ratings).map {
    line =>
      val Array(userId, productId, scoreStr) = line.split("::")
      AllRatedProducts(userId, prodId, scoreStr.toDouble)
 }
  
val ratings_new = ratings_parse.groupBy(_.prodId)
* 
*/
  val Retries = 3
  val prodRatings = getRandomProduct
    
  val productDict = new Dictionary(ratings.map(_.prodId).distinct.collect)

  val myRatingsRDD = sc.parallelize(prodRatings, 1)

  def getRandomProduct(Retries: Int) = getRandomProductID
  def getRandomProductID = {
    
  val randomProduct = (productDict.getWord(random.nextInt(productDict.size))).toString
  println("Printing randomProduct")
  println(randomProduct)
  if (randomProduct.size < 1) {
     throw new RuntimeException(s"Check your data please!")
  }
}
  
    val numRatings = ratings.count()
    val numUsers = ratings.map(_._2.user).distinct().count()
    val numProducts = ratings.map(_._2.product).distinct().count()

    println("Got " + numRatings + " ratings from "
      + numUsers + " users on " + numProducts + " products.")
  
      
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
      println("RMSE (validation) = " + validationRmse + " for the model trained with rank = " 
        + rank + ", lambda = " + lambda + ", and numIter = " + numIter + ".")
      if (validationRmse < bestValidationRmse) {
        bestModel = Some(model)
        bestValidationRmse = validationRmse
        bestRank = rank
        bestLambda = lambda
        bestNumIter = numIter
      }
    }

    
    val testRmse = computeRmse(bestModel.get, test, numTest)

    println("The best model was trained with rank = " + bestRank + " and lambda = " + bestLambda
      + ", and numIter = " + bestNumIter + ", and its RMSE on the test set is " + testRmse + ".")


    val meanRating = training.union(validation).map(_.rating).mean
    val baselineRmse = 
      math.sqrt(test.map(x => (meanRating - x.rating) * (meanRating - x.rating)).mean)
    val improvement = (baselineRmse - testRmse) / baselineRmse * 100
    println("The best model improves the baseline by " + "%1.2f".format(improvement) + "%.")
    
    
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
      println("%2d".format(i) + ": " + movies(r.product))
      i += 1
    }
      
      
    sc.stop()

//NEED TO CHANGE THE computeRmse() and get random product from dictionary and compare.

  /** Compute RMSE (Root Mean Squared Error). */
  def computeRmse(model: MatrixFactorizationModel, data: RDD[Rating], n: Long): Double = {
    val predictions: RDD[Rating] = model.predict(data.map(x => (x.user, x.product)))
    val predictionsAndRatings = predictions.map(x => ((x.user, x.product), x.rating))
      .join(data.map(x => ((x.user, x.product), x.rating)))
      .values
    math.sqrt(predictionsAndRatings.map(x => (x._1 - x._2) * (x._1 - x._2)).reduce(_ + _) / n)
  }
 
}  
  
/*
  @transient val random = new Random() with Serializable
  // first create an RDD out of the rating file
  val rawTrainingRatings = sc.textFile(ratingFile).map {
    line =>
      val Array(userId, productId, scoreStr) = line.split(",")
      AmazonRating(userId, productId, scoreStr.toDouble)
  }

  // only keep users that have rated between MinRecommendationsPerUser and MaxRecommendationsPerUser products
  val trainingRatings = rawTrainingRatings.groupBy(_.userId)
                                          .filter(r => MinRecommendationsPerUser <= r._2.size  && r._2.size < MaxRecommendationsPerUser)
                                          .flatMap(_._2)
                                          .repartition(NumPartitions)
                                          .cache()

  println(s"Parsed $ratingFile. Kept ${trainingRatings.count()} ratings out of ${rawTrainingRatings.count()}")
  
  // create user and item dictionaries
  val userDict = new Dictionary(MyUsername +: trainingRatings.map(_.userId).distinct.collect)
  val productDict = new Dictionary(trainingRatings.map(_.productId).distinct.collect)

  private def toSparkRating(amazonRating: AmazonRating) = {
    Rating(userDict.getIndex(amazonRating.userId),
      productDict.getIndex(amazonRating.productId),
      amazonRating.rating)
  }

  private def toAmazonRating(rating: Rating) = {
    AmazonRating(userDict.getWord(rating.user),
      productDict.getWord(rating.product),
      rating.rating
    )
  }

  // convert to Spark Ratings using the dictionaries
  val sparkRatings = trainingRatings.map(toSparkRating)

  def getRandomProductId = productDict.getWord(random.nextInt(productDict.size))

  def predict(ratings: Seq[AmazonRating]) = {
    // train model
    val myRatings = ratings.map(toSparkRating)
    val myRatingRDD = sc.parallelize(myRatings)

    val startAls = DateTime.now
    val model = ALS.train((sparkRatings ++ myRatingRDD).repartition(NumPartitions), 10, 20, 0.01)

    val myProducts = myRatings.map(_.product).toSet
    val candidates = sc.parallelize((0 until productDict.size).filterNot(myProducts.contains))

    // get ratings of all products not in my history ordered by rating (higher first) and only keep the first NumRecommendations
    val myUserId = userDict.getIndex(MyUsername)
    val recommendations = model.predict(candidates.map((myUserId, _))).collect
    val endAls = DateTime.now
    val result = recommendations.sortBy(-_.rating).take(NumRecommendations).map(toAmazonRating)
    val alsTime = Seconds.secondsBetween(startAls, endAls).getSeconds

    println(s"ALS Time: $alsTime seconds")
    result
  }
}
*/
