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
package models.stack


object MConfig {

  val sparkurl = play.api.Play.application(play.api.Play.current).configuration.getString("spark.url").get
  val hdfsuri = play.api.Play.application(play.api.Play.current).configuration.getString("hdfs.url").get
  val retailfile = play.api.Play.application(play.api.Play.current).configuration.getString("retailfile").get
  val productsfile = play.api.Play.application(play.api.Play.current).configuration.getString("productsfile").get
  val username = play.api.Play.application(play.api.Play.current).configuration.getString("username").get
  val recommand_ID = play.api.Play.application(play.api.Play.current).configuration.getString("product_id").get
}