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

import play.api.mvc._
import java.io.File
import scala.io.Source
import org.apache.log4j.Logger
import org.apache.log4j.Level

import models._

object Application extends Controller {  
  
  def index() = Action {
    Ok(views.html.index("Megam Analytics."))
  }
  
  def upload() = Action {
    models.HDFSFileService.saveFile("/home/rajthilak/Documents/csv/Clothing.csv")
    Ok(views.html.ratings("hello"))
  }
  
  def analysis() = Action {
    models.Retail.buyingbehaviour("TV", "retail5.csv")
    /*models.Retail.buyingbehaviour("TV", "retail5.csv") match {
      case Success(succ) => {
        
      }        
      case Failure(err) => {
              val rn: FunnelResponse = new HttpReturningError(err)
              Status(rn.code)(rn.toJson(true))
            }  
    } */
    Ok(views.html.ratings("hello"))
  }

}
