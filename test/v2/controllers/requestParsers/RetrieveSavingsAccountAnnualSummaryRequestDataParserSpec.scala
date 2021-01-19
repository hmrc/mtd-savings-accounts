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
import uk.gov.hmrc.domain.Nino
import v2.mocks.validators.MockRetrieveSavingsAccountAnnualSummaryValidator
import v2.models.errors.{BadRequestError, ErrorWrapper, NinoFormatError, TaxYearFormatError}
import v2.models.requestData.{DesTaxYear, RetrieveSavingsAccountAnnualSummaryRawData, RetrieveSavingsAccountAnnualSummaryRequest}

class RetrieveSavingsAccountAnnualSummaryRequestDataParserSpec extends UnitSpec {

  trait Test extends MockRetrieveSavingsAccountAnnualSummaryValidator {
    val parser = new RetrieveSavingsAccountAnnualSummaryRequestDataParser(mockValidator)
  }

  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino = "AA123456A"
  val invalidNino = "AA123A"
  val taxYear = "2018-19"
  val invalidTaxYear = "2018-2019"
  val savingsAccountId = "ASDFKLM123WQR13"
  val requestRawData: RetrieveSavingsAccountAnnualSummaryRawData = RetrieveSavingsAccountAnnualSummaryRawData(nino, taxYear, savingsAccountId)

  "parse request" should {
    "return a valid request details object" when {
      "a valid request details is supplied" in new Test{
        val validRequest: RetrieveSavingsAccountAnnualSummaryRequest =
          RetrieveSavingsAccountAnnualSummaryRequest(Nino(nino), DesTaxYear.fromMtd(taxYear), savingsAccountId)
        MockRetrieveSavingsAccountAnnualSummaryValidator.validate(requestRawData).returns(Nil)

        val result: Either[ErrorWrapper, RetrieveSavingsAccountAnnualSummaryRequest] = parser.parseRequest(requestRawData)
        result shouldBe Right(validRequest)
      }
    }

    "return a single error" when {
      "invalid nino is supplied" in new Test {
        val invalidRequestRawData: RetrieveSavingsAccountAnnualSummaryRawData = requestRawData.copy(nino = invalidNino)
        MockRetrieveSavingsAccountAnnualSummaryValidator.validate(invalidRequestRawData).returns(List(NinoFormatError))

        val result: Either[ErrorWrapper, RetrieveSavingsAccountAnnualSummaryRequest] = parser.parseRequest(invalidRequestRawData)
        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }
    }

    "return multiple errors" when {
      "invalid nino and invalid tax year is supplied" in new Test{
        val invalidRequestRawData: RetrieveSavingsAccountAnnualSummaryRawData = requestRawData.copy(nino = invalidNino, taxYear = invalidTaxYear)
        MockRetrieveSavingsAccountAnnualSummaryValidator.validate(invalidRequestRawData).returns(List(NinoFormatError, TaxYearFormatError))

        val result: Either[ErrorWrapper, RetrieveSavingsAccountAnnualSummaryRequest] = parser.parseRequest(invalidRequestRawData)
        result shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }
    }

  }
}
