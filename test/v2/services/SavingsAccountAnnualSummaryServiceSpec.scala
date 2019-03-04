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

package v2.services

import uk.gov.hmrc.domain.Nino
import v2.mocks.connectors.MockDesConnector
import v2.models.domain._
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData._

import scala.concurrent.Future

class SavingsAccountAnnualSummaryServiceSpec extends ServiceSpec {

  val incomeSourceId = "ZZIS12345678901"
  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino = "AA123456A"
  val taxYear = "2018-19"
  val transactionReference = "0000000000000001"

  val deaTaxYearFormatError = Error("INVALID_TAXYEAR", "doesn't matter")
  val desNinoFormatError = Error("INVALID_NINO", "doesn't matter")

  trait Test extends MockDesConnector {
    lazy val service = new SavingsAccountAnnualSummaryService(connector)
  }

  "create" when {
    val savingsAccountAnnualSummary = SavingsAccountAnnualSummary(Some(2000.99), Some(5000.50))
    val request = AmendSavingsAccountAnnualSummaryRequest(Nino(nino), DesTaxYear(taxYear), incomeSourceId, savingsAccountAnnualSummary)

    "valid data is passed" should {
      "return a valid response" in new Test {
        val expected = DesResponse(correlationId, AmendSavingsAccountAnnualSummaryResponse(incomeSourceId))
        MockedDesConnector.amendAnnualSummary(request).returns(Future.successful(Right(expected)))
        val result: AmendSavingsAccountAnnualSummaryOutcome = await(service.amend(request))
        result shouldBe Right(expected)
      }
    }

    "DES returns multiple errors" should {
      "return multiple errors" in new Test {
        val expected = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError)))
        val desResponse = DesResponse(correlationId, MultipleErrors(Seq(desNinoFormatError, deaTaxYearFormatError)))
        MockedDesConnector.amendAnnualSummary(request).returns(Future.successful(Left(desResponse)))
        val result: AmendSavingsAccountAnnualSummaryOutcome = await(service.amend(request))
        result shouldBe Left(ErrorWrapper(Some(correlationId), BadRequestError, Some(expected.responseData.errors)))
      }
    }

    "the connector returns an outbound error" should {
      "return that outbound error as-is" in new Test {
        val fakeError = Error("doesn't matter", "really doesn't matter")
        val desResponse = DesResponse(correlationId, OutboundError(fakeError))
        val expected = DesResponse(correlationId, OutboundError(fakeError))
        MockedDesConnector.amendAnnualSummary(request).returns(Future.successful(Left(desResponse)))
        val result: AmendSavingsAccountAnnualSummaryOutcome = await(service.amend(request))
        result shouldBe Left(ErrorWrapper(Some(correlationId), expected.responseData.error, None))
      }
    }

    "one of the errors from DES is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, DownstreamError)))
        MockedDesConnector.amendAnnualSummary(request).returns(Future.successful(Left(expected)))
        val result: AmendSavingsAccountAnnualSummaryOutcome = await(service.amend(request))
        result shouldBe Left(ErrorWrapper(Some(correlationId), DownstreamError, None))
      }
    }

    Map(
      "INVALID_TYPE" -> DownstreamError,
      "INVALID_NINO" -> NinoFormatError,
      "INVALID_TAXYEAR" -> TaxYearFormatError,
      "NOT_FOUND_INCOME_SOURCE" -> MatchingResourceNotFoundError,
      "INVALID_ACCOUNTING_PERIOD" -> RuleTaxYearNotSupportedError,
      "INVALID_PAYLOAD" -> BadRequestError,
      "MISSING_CHARITIES_NAME_GIFT_AID" -> DownstreamError,
      "MISSING_GIFT_AID_AMOUNT" -> DownstreamError,
      "MISSING_CHARITIES_NAME_INVESTMENT" -> DownstreamError,
      "MISSING_INVESTMENT_AMOUNT" -> DownstreamError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError,
      "UNEXPECTED_ERROR" -> DownstreamError
    ).foreach {
      case (desErrorCode, mtdError) =>
        s"DES returns a $desErrorCode error" should {
          s"return a ${mtdError.code} error" in new Test {
            val desResponse = DesResponse(correlationId, SingleError(Error(desErrorCode, "doesn't matter")))
            val expected = ErrorWrapper(Some(correlationId), mtdError, None)
            MockedDesConnector.amendAnnualSummary(request).returns(Future.successful(Left(desResponse)))
            val result: AmendSavingsAccountAnnualSummaryOutcome = await(service.amend(request))
            result shouldBe Left(expected)
          }
        }
    }
  }
}