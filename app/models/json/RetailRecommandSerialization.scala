/* 
** Copyright [2013-2014] [Megam Systems]
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
package models.json

import scalaz._
import scalaz.NonEmptyList._
import scalaz.Validation
import scalaz.Validation._
import Scalaz._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import java.util.Date
import java.nio.charset.Charset
import controllers.SerializationBase
import models.{ RetailRecommand }

/**
 * @author rajthilak
 *
 */


object RetailRecommandSerialization extends SerializationBase[RetailRecommand] {
  protected val ProductIdKey = "productId"
  protected val NoofOrdersKey = "nooforders" 
  

  override implicit val writer = new JSONW[RetailRecommand] {

    override def write(h: RetailRecommand): JValue = {
      JObject(
        JField(ProductIdKey, toJSON(h.productId)) ::
          JField(NoofOrdersKey, toJSON(h.nooforders)) ::          
          Nil)
    }
  }

  override implicit val reader = new JSONR[RetailRecommand] {

    override def read(json: JValue): Result[RetailRecommand] = {
      val productIdField = field[String](ProductIdKey)(json)
      val noofordersField = field[String](NoofOrdersKey)(json)      

      (productIdField |@| noofordersField ) {
        (productId: String, nooforders: String) =>
          new RetailRecommand(productId, nooforders)
      }
    }
  }
}