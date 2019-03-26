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

package v2.controllers.requestParsers.validators

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v2.models.errors._
import v2.models.requestData.CreateSavingsAccountRawData

class CreateSavingsAccountValidatorSpec extends UnitSpec {

  val validNino = "AA123456A"
  val json =
    """
      |{
      |    "accountName": "Main account name"
      |}
    """.stripMargin

  val validJsonBody = AnyContentAsJson(Json.parse(json))

  private trait Test {
    val validator = new CreateSavingsAccountValidator()
  }

  "running a validation" should {

    "return no errors" when {
      "the uri is valid and the JSON payload is Valid with all fields" in new Test {
        val inputData = CreateSavingsAccountRawData(validNino, validJsonBody)

        val result: Seq[MtdError] = validator.validate(inputData)

        result shouldBe List()

      }
    }

    "return nino format error" when {
      "an invalid nino is supplied" in {
        val nino = "AA1456A"
        val expectedData = List(NinoFormatError)
        val requestRawData = CreateSavingsAccountRawData(nino, validJsonBody)

        val result = new CreateSavingsAccountValidator().validate(requestRawData)

        result shouldBe expectedData
      }
    }

    "return bad request error" when {
      "empty body is supplied" in {
        val nino = "AA123456A"
        val expectedData = List(AccountNameMissingError)
        val emptyJson =
          """
            |{
            |}
          """.stripMargin

        val requestRawData = CreateSavingsAccountRawData(nino, AnyContentAsJson(Json.parse(emptyJson)))

        val result = new CreateSavingsAccountValidator().validate(requestRawData)

        result shouldBe expectedData
      }
    }

    "return account format error" when {
      "an invalid account name is supplied" in {
        val nino = "AA123456A"
        val expectedData = List(AccountNameFormatError)
        val invalidJson =
          """
            |{
            |"accountName": "Main*account^name"
            |}
          """.stripMargin

        val requestRawData = CreateSavingsAccountRawData(nino, AnyContentAsJson(Json.parse(invalidJson)))

        val result = new CreateSavingsAccountValidator().validate(requestRawData)

        result shouldBe expectedData
      }
    }
  }

}
