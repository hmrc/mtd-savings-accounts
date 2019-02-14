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

import play.api.libs.json.Json
import support.UnitSpec
import v2.models.utils.JsonErrorValidators

class SavingsAccountsSpec extends UnitSpec with JsonErrorValidators{

  val mtdFormatJson = Json.parse(
    """
      |{
      |    "id": "SAVKB2UVwUTBQGJ",
      |    "accountName": "Main account name"
      |}
    """.stripMargin)

  "reads" should {
    "return a valid SavingsAccounts object" when {
      "passed valid JSON" in {
        val json =
          """
            |{
            |    "accountName": "Main account name"
            |}
          """.stripMargin

        val model = SavingsAccounts(id = None, accountName = Some("Main account name"))

        Json.parse(json).as[SavingsAccounts] shouldBe model
      }
    }

    "return empty savings object" when {
      "passed an empty SavingsAccounts object" in {
        val json =
          """
            |{}
          """.stripMargin

        Json.parse(json).as[SavingsAccounts] shouldBe SavingsAccounts(None, None)
      }
    }

    testPropertyType[SavingsAccounts](mtdFormatJson)(
      path = "/accountName",
      replacement = 12344.toJson,
      expectedError = JsonError.STRING_FORMAT_EXCEPTION
    )

    testPropertyType[SavingsAccounts](mtdFormatJson)(
      path = "/id",
      replacement = 123.toJson,
      expectedError = JsonError.STRING_FORMAT_EXCEPTION
    )
  }

  "desWrites" should {
    "return valid des formatted JSON" when {
      "passed a valid MTD SavingsAccounts object" in {
        val json =
          """
            |{
            |    "incomeSourceType": "interest-from-uk-banks",
            |    "incomeSourceName": "Main account name"
            |}
          """.stripMargin

        val model = SavingsAccounts(None, accountName = Some("Main account name"))

        Json.parse(json) shouldBe SavingsAccounts.desWrites.writes(model)
      }
    }

    "return JSON with only incomeSourceType" when {
      "passed a MTD SavingsAccounts object with no accountName" in {
        val json =
          """
            |{
            |    "incomeSourceType": "interest-from-uk-banks"
            |}
          """.stripMargin

        val model = SavingsAccounts(None, None)

        SavingsAccounts.desWrites.writes(model) shouldBe Json.parse(json)
      }
    }
  }

  "desReads" should {
    "return a valid MTD SavingsAccounts object" when {
      "passed valid des JSON" in {
        val json =
          """
            |{
            |    "incomeSourceId": "SAVKB2UVwUTBQGJ"
            |}
          """.stripMargin

        val model = SavingsAccounts(id = Some("SAVKB2UVwUTBQGJ"), None)

        Json.parse(json).as[SavingsAccounts](SavingsAccounts.desReads) shouldBe model
      }
    }
  }
}
