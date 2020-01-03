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

import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v2.mocks.validators.MockRetrieveSavingsAccountValidator
import v2.models.errors._
import v2.models.requestData.{RetrieveSavingsAccountRawData, RetrieveSavingsAccountRequest}

class RetrieveSavingsAccountRequestDataParserSpec extends UnitSpec {

  val validNino = "AA123456A"
  val correlationId = "X-123"
  val accountId = "SAVKB2UVwUTBQGJ"

  trait Test extends MockRetrieveSavingsAccountValidator {
    lazy val parser = new RetrieveSavingsAccountRequestDataParser(mockValidator)
  }


  "parse" should {

    "return a retrieve savings account request object" when {
      "valid request data is supplied" in new Test {

        val retrieveSavingsAccountRawData =
          RetrieveSavingsAccountRawData(validNino, accountId)

        val retrieveSavingsAccountRequest =
          RetrieveSavingsAccountRequest(Nino(validNino), accountId)

        MockedCreateSavingsAccountValidator.validate(retrieveSavingsAccountRawData)
          .returns(List())

        parser.parseRequest(retrieveSavingsAccountRawData) shouldBe Right(retrieveSavingsAccountRequest)
      }
    }

    "return an ErrorWrapper" when {

      val invalidNino = "AA112A"

      "a single validation error occurs" in new Test {
        val retrieveSavingsAccountRawData =
          RetrieveSavingsAccountRawData(invalidNino, accountId)

        val expectedResponse =
          ErrorWrapper(None, NinoFormatError, None)

        MockedCreateSavingsAccountValidator.validate(retrieveSavingsAccountRawData)
          .returns(List(NinoFormatError))

        parser.parseRequest(retrieveSavingsAccountRawData) shouldBe Left(expectedResponse)
      }

      "multiple validation errors occur" in new Test {
        val retrieveSavingsAccountRawData =
          RetrieveSavingsAccountRawData(validNino, accountId)

        val multipleErrorWrapper =
          ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, AccountIdFormatError)))

        MockedCreateSavingsAccountValidator.validate(retrieveSavingsAccountRawData)
          .returns(List(NinoFormatError, AccountIdFormatError))


        parser.parseRequest(retrieveSavingsAccountRawData) shouldBe Left(multipleErrorWrapper)
      }
    }

  }
}
