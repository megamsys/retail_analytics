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
package controllers

import scalaz._
import Scalaz._
import scalaz.EitherT._
import scalaz.Validation
//import scalaz.Validation.FlatMap._
import scalaz.NonEmptyList._
import play.api.mvc._
import java.io.File
import scala.io.Source
import org.apache.log4j.Logger
import org.apache.log4j.Level
import models._
import models.stack._

import play.api.libs.json._

object Application extends Controller {

  def index() = Action { implicit request =>
    Ok(views.html.index("Megam Analytics."))
  }

  def upload = Action(parse.multipartFormData) { implicit request =>
       request.body.file("picture").map { picture =>
       import java.io.File
      val filename = picture.filename
      val contentType = picture.contentType
      picture.ref.moveTo(new File("/tmp/"+filename))
     
      models.HDFSFileService.saveFile("/tmp/"+filename) match {
        case Success(succ) => {
          val fu = List(("success" -> succ))
          Redirect("/").flashing(fu: _*)
        }
        case Failure(err) => {
          val fu = List(("error" -> "File doesn't get uploaded"))
          Redirect("/").flashing(fu: _*)
        }
      }
    }.getOrElse {
      val fu = List(("error" -> "File doesn't get uploaded.."))
      Redirect("/").flashing(fu: _*)
    }
  }

  def analysis() = Action { implicit request =>
  val tuple_res = models.Retail.buyingbehaviour(MConfig.recommand_ID.toInt, MConfig.retailfile)
  
    /*models.Retail.buyingbehaviour("TV", "retail5.csv") match {
      case Success(succ) => {
        
      }        
      case Failure(err) => {
              val rn: FunnelResponse = new HttpReturningError(err)
              Status(rn.code)(rn.toJson(true))
            }  
    }*/ 
  println("BACK==========================>>>")
  println(tuple_res._1)
 
  
  //val finalJson = {
  //  for {
  //    product <- productList
  //  } yield Json.parse(product).as[JsObject]
 // }
    Ok(views.html.finalProducts(tuple_res._1, tuple_res._2))
  }

}
