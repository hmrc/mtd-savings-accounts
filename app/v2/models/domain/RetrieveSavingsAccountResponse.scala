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

import play.api.libs.json._

case class RetrieveSavingsAccountResponse(accountName: String)

object RetrieveSavingsAccountResponse {
  // Note that we read the array of accounts from DES into a list.
  // This list may have no entries (when account does not exist)
  // or more than one entry
  implicit val desReads: Reads[RetrieveSavingsAccountResponse] =
    (__ \ "incomeSourceName").read[String].map(RetrieveSavingsAccountResponse.apply)

  implicit val vendorWrites: OWrites[RetrieveSavingsAccountResponse] = Json.writes[RetrieveSavingsAccountResponse]
}