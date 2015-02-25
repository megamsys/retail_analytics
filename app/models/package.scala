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
import scalaz._
import Scalaz._
//import scalaz.effect.IO
import scalaz.EitherT._
import scalaz.Validation
//import scalaz.Validation.FlatMap._
import scalaz.NonEmptyList._
import models.json._

import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import java.nio.charset.Charset

/**
 * @author rajthilak
 *
 */
package object models {


  type RetailRecommands = List[RetailRecommand]

  object RetailRecommands {
    val emptyRR = List(RetailRecommand.empty)
    def toJValue(nres: RetailRecommands): JValue = {

      import net.liftweb.json.scalaz.JsonScalaz.toJSON
      import models.json.RetailRecommandsSerialization.{ writer => RetailRecommandsWriter }
      toJSON(nres)(RetailRecommandsWriter)
    }

    def fromJValue(jValue: JValue)(implicit charset: Charset = Charset.forName("UTF-8")): Result[RetailRecommands] = {
      import net.liftweb.json.scalaz.JsonScalaz.fromJSON
      import models.json.RetailRecommandsSerialization.{ reader => RetailRecommandsReader }
      fromJSON(jValue)(RetailRecommandsReader)
    }

    def toJson(nres: RetailRecommands, prettyPrint: Boolean = false): String = if (prettyPrint) {
      pretty(render(toJValue(nres)))
    } else {
      compactRender(toJValue(nres))
    }
    
    def apply(plansList: List[RetailRecommand]): RetailRecommands = plansList

    def empty: List[RetailRecommand] = emptyRR

  } 
  
  
}