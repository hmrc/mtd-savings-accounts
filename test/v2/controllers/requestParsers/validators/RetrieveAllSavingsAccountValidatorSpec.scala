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
import v2.models.requestData.RetrieveAllSavingsAccountRawData

class RetrieveAllSavingsAccountValidatorSpec extends UnitSpec {

  val validNino = "AA123456A"

  private trait Test {
    val validator = new RetrieveAllSavingsAccountValidator()
  }

  "running a validation" when {

    "the uri is valid and the JSON payload is Valid with all fields" should {
      "return no errors" in new Test {
        val inputData = RetrieveAllSavingsAccountRawData(validNino)

        val result: Seq[Error] = validator.validate(inputData)

        result shouldBe List()

      }
    }

    "an invalid nino is supplied" should {
      "return nino format error" in {
        val nino = "AA1456A"
        val expectedData = List(NinoFormatError)
        val requestRawData = RetrieveAllSavingsAccountRawData(nino)

        val result = new RetrieveAllSavingsAccountValidator().validate(requestRawData)

        result shouldBe expectedData
      }
    }
  }

}
