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
import Scalaz._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import controllers.SerializationBase
import models._
import java.nio.charset.Charset
/**
 * @author rajthilak
 *
 */
object RetailRecommandsSerialization extends SerializationBase[RetailRecommands] {

  protected val JSONClazKey = "json_claz"
  protected val ResultsKey = "plans"

  implicit override val writer = new JSONW[RetailRecommands] {
    override def write(h: RetailRecommands): JValue = {
      val nrsList: Option[List[JValue]] = h.map {
        nrOpt: RetailRecommand => nrOpt.toJValue
      }.some
      
      JArray(nrsList.getOrElse(List.empty[JValue]))
    }
  }

  implicit override val reader = new JSONR[RetailRecommands] {
    override def read(json: JValue): Result[RetailRecommands] = {
      json match {
        case JArray(jObjectList) => {
          val list = jObjectList.flatMap { jValue: JValue =>
            RetailRecommand.fromJValue(jValue) match {
              case Success(nr) => List(nr)
              case Failure(fail) => List[RetailRecommand]()
            }
          }.some

          val nrs: RetailRecommands = RetailRecommands(list.getOrElse(RetailRecommands.empty))
          nrs.successNel[Error]
        }
        case j => UnexpectedJSONError(j, classOf[JArray]).failureNel[RetailRecommands]
      }
    }
  }
}