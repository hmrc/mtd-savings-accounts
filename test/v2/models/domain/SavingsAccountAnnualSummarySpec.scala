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

package v2.models.domain

import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v2.models.utils.JsonErrorValidators

class SavingsAccountAnnualSummarySpec  extends UnitSpec with JsonErrorValidators {

  val json: JsValue = Json.parse(
    """
      |{
      |   "taxedUkInterest": 2000.99,
      |   "untaxedUkInterest": 5000.50
      |}
    """.stripMargin)

  "reads" when {
    "passed valid JSON" should {
      "return a valid SavingsAccountAnnualSummary object" in {

        val model = SavingsAccountAnnualSummary(Some(2000.99), Some(5000.50))

        json.as[SavingsAccountAnnualSummary] shouldBe model
      }
    }

    "passed JSON with only taxedUKInterest" should {
      "return a valid SavingsAccountAnnualSummary object" in {

        val mtdJson: JsValue = Json.parse(
          """
            |{
            |   "taxedUkInterest": 2000.99
            |}
          """.stripMargin)
        val model = SavingsAccountAnnualSummary(Some(2000.99), None)

        mtdJson.as[SavingsAccountAnnualSummary] shouldBe model
      }
    }

    "passed JSON with only untaxedUKInterest" should {
      "return a valid SavingsAccountAnnualSummary object" in {

        val mtdJson: JsValue = Json.parse(
          """
            |{
            |   "untaxedUkInterest": 5000.50
            |}
          """.stripMargin)
        val model = SavingsAccountAnnualSummary(None, Some(5000.50))

        mtdJson.as[SavingsAccountAnnualSummary] shouldBe model
      }
    }

    testPropertyType[SavingsAccountAnnualSummary](json)(
      path = "/taxedUkInterest",
      replacement = "test".toJson,
      expectedError = JsonError.NUMBER_FORMAT_EXCEPTION
    )

    testPropertyType[SavingsAccountAnnualSummary](json)(
      path = "/untaxedUkInterest",
      replacement = "test".toJson,
      expectedError = JsonError.NUMBER_FORMAT_EXCEPTION
    )
  }

  "writes" when {
    "passed a valid SavingsAccountAnnualSummary object" should {
      "return valid des formatted JSON" in {
        val json =
          """
            |{
            |   "taxedUkInterest": 2000.99,
            |   "untaxedUkInterest": 5000.50
            |}
          """.stripMargin

        val model = SavingsAccountAnnualSummary(Some(2000.99), Some(5000.50))

        SavingsAccountAnnualSummary.writes.writes(model) shouldBe Json.parse(json)
      }
    }
  }
}
