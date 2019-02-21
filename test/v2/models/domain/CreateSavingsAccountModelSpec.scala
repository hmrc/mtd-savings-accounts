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
import support.UnitSpec
import v2.models.utils.JsonErrorValidators

class CreateSavingsAccountModelSpec extends UnitSpec with JsonErrorValidators {

  val mtdFormatJson: JsValue = Json.parse(
    """
      |{
      |    "accountName": "Main account name"
      |}
    """.stripMargin)

  "reads" when {
    "passed valid JSON" should {
      "return a valid SavingsAccount object" in {

        val model = CreateSavingsAccount(accountName = "Main account name")

        mtdFormatJson.as[CreateSavingsAccount] shouldBe model
      }
    }

    testMandatoryProperty[CreateSavingsAccount](mtdFormatJson)("/accountName")

    testPropertyType[CreateSavingsAccount](mtdFormatJson)(
      path = "/accountName",
      replacement = 12344.toJson,
      expectedError = JsonError.STRING_FORMAT_EXCEPTION
    )
  }

  "writes" when {
    "passed a valid MTD SavingsAccount object" should {
      "return valid des formatted JSON" in {
        val json =
          """
            |{
            |    "incomeSourceType": "interest-from-uk-banks",
            |    "incomeSourceName": "Main account name"
            |}
          """.stripMargin

        val model = CreateSavingsAccount(accountName = "Main account name")

        Json.parse(json) shouldBe CreateSavingsAccount.writes.writes(model)
      }
    }
  }
}
