/*
 * Copyright 2020 HM Revenue & Customs
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

package v2.fixtures

import v2.models.domain.CreateSavingsAccountRequest
import play.api.libs.json.{JsArray, JsValue, Json}
import v2.models.errors._

object Fixtures {

  object SavingsAccountsFixture {
    val createJson: JsValue = Json.parse(
      """
        |{
        |   "accountName": "Main account name"
        |}
      """.stripMargin
    )
    val createJsonResponse: String => JsValue = id => Json.parse(
      s"""
         |{
         |  "id": "$id"
         |}
      """.stripMargin
    )

    val retrieveAllJsonReponse: (String, String) => JsValue = (id, accountName) => Json.obj(
      "savingsAccounts" -> JsArray(Seq(
        Json.obj(
          "id" -> id,
          "accountName" -> accountName
        )
      ))
    )

    val retrieveAnnualJsonResponse: (String, String) => JsValue = (taxedUkInterest, untaxedUkInterest) => Json.parse(
      s"""{
         |"taxedUkInterest": $taxedUkInterest,
         |"untaxedUkInterest": $untaxedUkInterest
         |}
      """.stripMargin
    )

    val retrieveJsonReponse: String => JsValue = accountName => Json.obj(
      "accountName" -> accountName
    )

    def amendRequestJson(taxedUkInterest: BigDecimal  = 123.45, untaxedUkInterest: BigDecimal = 543.21) = Json.parse(
      s"""{
        |"taxedUkInterest": $taxedUkInterest,
        |"untaxedUkInterest": $untaxedUkInterest
        |}
      """.stripMargin)

    val multipleErrorsFromParserJson: String => JsValue = correlationId => Json.toJson(
      ErrorWrapper(Some(correlationId), BadRequestError, Some(Seq(AccountNameMissingError, AccountNameFormatError)))
    )
    val multipleErrorsFromServerJson: String => JsValue = correlationId => Json.toJson(
      Json.toJson(ErrorWrapper(Some(correlationId), BadRequestError, Some(Seq(AccountNameDuplicateError, MaximumSavingsAccountsLimitError))))
    )
    val downstreamErrorJson: JsValue = Json.toJson(DownstreamError)

    val createSavingsAccountRequestModel = CreateSavingsAccountRequest("Main account name")
  }

}
