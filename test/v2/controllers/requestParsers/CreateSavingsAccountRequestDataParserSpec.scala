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

package v2.controllers.requestParsers

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v2.controllers.requestParsers.validators.validations.JsonFormatValidation
import v2.mocks.validators.MockCreateSavingsAccountValidator
import v2.models.domain.CreateSavingsAccountRequest
import v2.models.errors._
import v2.models.requestData.{CreateSavingsAccountRawData, CreateSavingsAccountRequestData}

class CreateSavingsAccountRequestDataParserSpec extends UnitSpec {

  val validNino = "AA123456A"
  val json =
    """
      |{
      |    "accountName": "Main account name"
      |}
    """.stripMargin
  val validJsonBody = AnyContentAsJson(Json.parse(json))
  val correlationId = "X-123"

  val model = CreateSavingsAccountRequest(accountName = "Main account name")

  trait Test extends MockCreateSavingsAccountValidator {
    lazy val parser = new CreateSavingsAccountRequestDataParser(mockValidator)
  }


  "parse" should {

    "return an create savings account request object" when {
      "valid request data is supplied" in new Test {

        val createSavingsAccountRequestData =
          CreateSavingsAccountRawData(validNino, validJsonBody)

        val createSavingsAccountRequest =
          CreateSavingsAccountRequestData(Nino(validNino), model)

        MockedCreateSavingsAccountValidator.validate(createSavingsAccountRequestData)
          .returns(List())

        parser.parseRequest(createSavingsAccountRequestData) shouldBe Right(createSavingsAccountRequest)
      }
    }

    "return an ErrorWrapper" when {

      val invalidNino = "AA112A"

      "a single validation error occurs" in new Test {
        val createSavingsAccountRequestData =
          CreateSavingsAccountRawData(invalidNino, validJsonBody)

        val expectedResponse =
          ErrorWrapper(None, NinoFormatError, None)

        MockedCreateSavingsAccountValidator.validate(createSavingsAccountRequestData)
          .returns(List(NinoFormatError))

        val receivedResponse = parser.parseRequest(createSavingsAccountRequestData)
        expectedResponse.copy()
        receivedResponse shouldBe Left(expectedResponse)
      }

      "multiple validation errors occur" in new Test {
        val createSavingsAccountRequestData =
          CreateSavingsAccountRawData(validNino, validJsonBody)

        val multipleErrorWrapper =
          ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, AccountNameDuplicateError)))

        MockedCreateSavingsAccountValidator.validate(createSavingsAccountRequestData)
          .returns(List(NinoFormatError, AccountNameDuplicateError))


        parser.parseRequest(createSavingsAccountRequestData) shouldBe Left(multipleErrorWrapper)
      }

      "return a JSON validation error" when {
        "the supplied JSON fails validation" in new Test {
          val createSavingsAccountRawData = CreateSavingsAccountRawData(validNino, validJsonBody)
          private val expectedError = Error(JsonFormatValidation.JSON_NUMBER_EXPECTED, "/fieldName should be a valid JSON number")

          MockedCreateSavingsAccountValidator.validate(createSavingsAccountRawData)
            .returns(List(expectedError))
          private val result = parser.parseRequest(createSavingsAccountRawData)

          result shouldBe Left(ErrorWrapper(None, BadRequestError, Some(List(expectedError))))
        }
      }

    }

  }
}
