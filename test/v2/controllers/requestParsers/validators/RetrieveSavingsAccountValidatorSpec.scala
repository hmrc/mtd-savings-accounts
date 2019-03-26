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

import support.UnitSpec
import v2.models.errors._
import v2.models.requestData.RetrieveSavingsAccountRawData

class RetrieveSavingsAccountValidatorSpec extends UnitSpec {

  private trait Test {
    val validator = new RetrieveSavingsAccountValidator()
  }

  "running a validation" when {

    "the uri is valid and the JSON payload is Valid with all fields" should {
      "return no errors" in new Test {
        val validNino = "AA123456A"
        val accountId = "SAVKB2UVwUTBQGJ"
        val inputData = RetrieveSavingsAccountRawData(validNino, accountId)

        val result: Seq[MtdError] = validator.validate(inputData)

        result shouldBe List()

      }
    }

    "an invalid nino is supplied" should {
      "return nino format error" in {
        val nino = "AA1456A"
        val accountId = "SAVKB2UVwUTBQGJ"
        val expectedData = List(NinoFormatError)
        val requestRawData = RetrieveSavingsAccountRawData(nino, accountId)

        val result = new RetrieveSavingsAccountValidator().validate(requestRawData)

        result shouldBe expectedData
      }
    }

    "an invalid accountId is supplied" should {
      "return accountId format error" in {
        val nino = "AA123456A"
        val accountId = "SAVKB2UVwUTBQGJwqwkqewqeqwewqewqe21wqasfasffasas"
        val expectedData = List(AccountIdFormatError)
        val requestRawData = RetrieveSavingsAccountRawData(nino, accountId)

        val result = new RetrieveSavingsAccountValidator().validate(requestRawData)

        result shouldBe expectedData
      }
    }

    "an invalid accountId and nino is supplied" should {
      "return multiple errors" in {
        val nino = "AA1456A"
        val accountId = "SAVKB2UVwUTBQGJwqwkqewqeqwewqewqe21wqasfasffasas"
        val expectedData = List(NinoFormatError, AccountIdFormatError)
        val requestRawData = RetrieveSavingsAccountRawData(nino, accountId)

        val result = new RetrieveSavingsAccountValidator().validate(requestRawData)

        result shouldBe expectedData
      }
    }
  }

}
