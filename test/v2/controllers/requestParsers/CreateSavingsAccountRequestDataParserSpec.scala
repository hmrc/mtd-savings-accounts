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

package v2.controllers.requestParsers

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v2.mocks.validators.MockCreateSavingsAccountValidator
import v2.models.domain.CreateSavingsAccountRequest
import v2.models.errors.{AccountNameDuplicateError, BadRequestError, ErrorWrapper, NinoFormatError}
import v2.models.requestData.{CreateSavingsAccountRawData, CreateSavingsAccountRequestData}

class CreateSavingsAccountRequestDataParserSpec extends UnitSpec {

  val validNino = "AA123456A"
  val json: String =
    """
      |{
      |    "accountName": "Main account name"
      |}
    """.stripMargin
  val validJsonBody: AnyContentAsJson = AnyContentAsJson(Json.parse(json))
  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val model: CreateSavingsAccountRequest = CreateSavingsAccountRequest(accountName = "Main account name")

  trait Test extends MockCreateSavingsAccountValidator {
    lazy val parser = new CreateSavingsAccountRequestDataParser(mockValidator)
  }


  "parse" should {

    "return an create savings account request object" when {
      "valid request data is supplied" in new Test {

        val createSavingsAccountRequestData: CreateSavingsAccountRawData =
          CreateSavingsAccountRawData(validNino, validJsonBody)

        val createSavingsAccountRequest: CreateSavingsAccountRequestData =
          CreateSavingsAccountRequestData(Nino(validNino), model)

        MockedCreateSavingsAccountValidator.validate(createSavingsAccountRequestData)
          .returns(List())

        parser.parseRequest(createSavingsAccountRequestData) shouldBe Right(createSavingsAccountRequest)
      }
    }

    "return an ErrorWrapper" when {

      val invalidNino = "AA112A"

      "a single validation error occurs" in new Test {
        val createSavingsAccountRequestData: CreateSavingsAccountRawData =
          CreateSavingsAccountRawData(invalidNino, validJsonBody)

        val expectedResponse: ErrorWrapper =
          ErrorWrapper(correlationId, NinoFormatError, None)

        MockedCreateSavingsAccountValidator.validate(createSavingsAccountRequestData)
          .returns(List(NinoFormatError))

        val receivedResponse: Either[ErrorWrapper, CreateSavingsAccountRequestData] = parser.parseRequest(createSavingsAccountRequestData)
        expectedResponse.copy()
        receivedResponse shouldBe Left(expectedResponse)
      }

      "multiple validation errors occur" in new Test {
        val createSavingsAccountRequestData: CreateSavingsAccountRawData =
          CreateSavingsAccountRawData(validNino, validJsonBody)

        val multipleErrorWrapper: ErrorWrapper =
          ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, AccountNameDuplicateError)))

        MockedCreateSavingsAccountValidator.validate(createSavingsAccountRequestData)
          .returns(List(NinoFormatError, AccountNameDuplicateError))


        parser.parseRequest(createSavingsAccountRequestData) shouldBe Left(multipleErrorWrapper)
      }
    }

  }
}
