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

package v2.models.des

import play.api.libs.json.Json
import support.UnitSpec

class DesRetrieveSavingsAccountAnnualIncomeResponseSpec extends UnitSpec {

  "DesRetrieveSavingsAccountAnnualIncomeResponse reads" should {
    "parse from DES json response" in {
      val response = Json.parse(
        """
          |{
          |  "savingsInterestAnnualIncome": [
          |    {
          |      "incomeSourceId": "122784545874145",
          |      "taxedUkInterest": 93556675358.99,
          |      "untaxedUkInterest": 34514974058.99
          |    }
          |  ]
          |}
        """.stripMargin)

      response.as[DesRetrieveSavingsAccountAnnualIncomeResponse] shouldBe
        DesRetrieveSavingsAccountAnnualIncomeResponse(Seq(
          DesSavingsInterestAnnualIncome(
            incomeSourceId = "122784545874145",
            taxedUkInterest = Some(93556675358.99),
            untaxedUkInterest = Some(34514974058.99)
          )
        ))
    }
  }
}
