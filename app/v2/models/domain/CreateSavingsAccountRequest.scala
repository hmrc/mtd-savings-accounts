/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v2.models.domain

import play.api.libs.json.Writes._
import play.api.libs.json._

case class CreateSavingsAccountRequest(accountName: String)

object CreateSavingsAccountRequest {
  implicit val reads: Reads[CreateSavingsAccountRequest] = Json.reads[CreateSavingsAccountRequest]

  implicit val writes: Writes[CreateSavingsAccountRequest] = new Writes[CreateSavingsAccountRequest] {
    override def writes(o: CreateSavingsAccountRequest): JsValue = Json.obj(
      "incomeSourceType" -> "interest-from-uk-banks",
      "incomeSourceName" -> o.accountName
    )
  }
}
