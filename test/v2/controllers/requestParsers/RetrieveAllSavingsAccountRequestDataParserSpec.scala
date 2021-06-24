/*
 * Copyright 2021 HM Revenue & Customs
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

import support.UnitSpec
import v2.mocks.validators.MockRetrieveAllSavingsAccountValidator
import v2.models.domain.Nino
import v2.models.errors.{AccountNameDuplicateError, BadRequestError, ErrorWrapper, NinoFormatError}
import v2.models.requestData.{RetrieveAllSavingsAccountRawData, RetrieveAllSavingsAccountRequest}

class RetrieveAllSavingsAccountRequestDataParserSpec extends UnitSpec {

  val validNino = "AA123456A"
  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  trait Test extends MockRetrieveAllSavingsAccountValidator {
    lazy val parser = new RetrieveAllSavingsAccountRequestDataParser(mockValidator)
  }


  "parse" should {

    "return a retrieveAll savings account request object" when {
      "valid request data is supplied" in new Test {

        val retrieveSavingsAccountRawData: RetrieveAllSavingsAccountRawData =
          RetrieveAllSavingsAccountRawData(validNino)

        val retrieveSavingsAccountRequest: RetrieveAllSavingsAccountRequest =
          RetrieveAllSavingsAccountRequest(Nino(validNino))

        MockedCreateSavingsAccountValidator.validate(retrieveSavingsAccountRawData)
          .returns(List())

        parser.parseRequest(retrieveSavingsAccountRawData) shouldBe Right(retrieveSavingsAccountRequest)
      }
    }

    "return an ErrorWrapper" when {

      val invalidNino = "AA112A"

      "a single validation error occurs" in new Test {
        val retrieveSavingsAccountRawData: RetrieveAllSavingsAccountRawData =
          RetrieveAllSavingsAccountRawData(invalidNino)

        val expectedResponse: ErrorWrapper =
          ErrorWrapper(correlationId, NinoFormatError, None)

        MockedCreateSavingsAccountValidator.validate(retrieveSavingsAccountRawData)
          .returns(List(NinoFormatError))

        parser.parseRequest(retrieveSavingsAccountRawData) shouldBe Left(expectedResponse)
      }

      "multiple validation errors occur" in new Test {
        val retrieveSavingsAccountRawData: RetrieveAllSavingsAccountRawData =
          RetrieveAllSavingsAccountRawData(validNino)

        val multipleErrorWrapper: ErrorWrapper =
          ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, AccountNameDuplicateError)))

        MockedCreateSavingsAccountValidator.validate(retrieveSavingsAccountRawData)
          .returns(List(NinoFormatError, AccountNameDuplicateError))


        parser.parseRequest(retrieveSavingsAccountRawData) shouldBe Left(multipleErrorWrapper)
      }
    }

  }
}
