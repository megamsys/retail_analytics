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
