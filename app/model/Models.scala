package model

import play.api.libs.json.Json


//case class Product(itemId: String, title: String, url: String, img: String, description: String)
case class AllRatedProducts(userId: String, prodId: String, rating: Double)

//case class AmazonProductAndRating(product: AmazonProduct, rating: AmazonRating)

/* For MongoDB
object AmazonRating {
  implicit val amazonRatingHandler = Macros.handler[AmazonRating]
  implicit val amazonRatingFormat = Json.format[AmazonRating]
}
*/
