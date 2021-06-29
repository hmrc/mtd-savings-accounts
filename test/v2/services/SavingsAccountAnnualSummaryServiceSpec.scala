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

package v2.services

import v2.mocks.connectors.MockDesConnector
import v2.models.des.{DesAmendSavingsAccountAnnualSummaryResponse, DesRetrieveSavingsAccountAnnualIncomeResponse, DesSavingsInterestAnnualIncome}
import v2.models.domain.{Nino, _}
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData._

import scala.concurrent.Future

class SavingsAccountAnnualSummaryServiceSpec extends ServiceSpec {

  val incomeSourceId = "ZZIS12345678901"
  val nino = "AA123456A"
  val taxYear = "2018-19"
  val transactionReference = "0000000000000001"

  val deaTaxYearFormatError: Error = Error("INVALID_TAXYEAR", "doesn't matter")
  val desNinoFormatError: Error = Error("INVALID_NINO", "doesn't matter")

  trait Test extends MockDesConnector {
    lazy val service = new SavingsAccountAnnualSummaryService(connector)
  }

  "create" when {
    val savingsAccountAnnualSummary = SavingsAccountAnnualSummary(Some(2000.99), Some(5000.50))
    val request = AmendSavingsAccountAnnualSummaryRequest(Nino(nino), DesTaxYear.fromMtd(taxYear), incomeSourceId, savingsAccountAnnualSummary)

    "valid data is passed" should {
      "return a valid response" in new Test {
        val expected: DesResponse[DesAmendSavingsAccountAnnualSummaryResponse] =
          DesResponse(correlationId, DesAmendSavingsAccountAnnualSummaryResponse(incomeSourceId))
        MockedDesConnector.amendAnnualSummary(request).returns(Future.successful(Right(expected)))
        val result: AmendSavingsAccountAnnualSummaryOutcome = await(service.amend(request))
        result shouldBe Right(expected)
      }
    }

    "DES returns multiple errors" should {
      "return multiple errors" in new Test {
        val expected: DesResponse[MultipleErrors] = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError)))
        val desResponse: DesResponse[MultipleErrors] = DesResponse(correlationId, MultipleErrors(Seq(desNinoFormatError, deaTaxYearFormatError)))
        MockedDesConnector.amendAnnualSummary(request).returns(Future.successful(Left(desResponse)))
        val result: AmendSavingsAccountAnnualSummaryOutcome = await(service.amend(request))
        result shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(expected.responseData.errors)))
      }
    }

    "the connector returns an outbound error" should {
      "return that outbound error as-is" in new Test {
        val fakeError: Error = Error("doesn't matter", "really doesn't matter")
        val desResponse: DesResponse[OutboundError] = DesResponse(correlationId, OutboundError(fakeError))
        val expected: DesResponse[OutboundError] = DesResponse(correlationId, OutboundError(fakeError))
        MockedDesConnector.amendAnnualSummary(request).returns(Future.successful(Left(desResponse)))
        val result: AmendSavingsAccountAnnualSummaryOutcome = await(service.amend(request))
        result shouldBe Left(ErrorWrapper(correlationId, expected.responseData.error, None))
      }
    }

    "one of the errors from DES is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected: DesResponse[MultipleErrors] = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, DownstreamError)))
        MockedDesConnector.amendAnnualSummary(request).returns(Future.successful(Left(expected)))
        val result: AmendSavingsAccountAnnualSummaryOutcome = await(service.amend(request))
        result shouldBe Left(ErrorWrapper(correlationId, DownstreamError, None))
      }
    }

    Map(
      "INVALID_TYPE" -> DownstreamError,
      "INVALID_NINO" -> NinoFormatError,
      "INVALID_TAXYEAR" -> TaxYearFormatError,
      "NOT_FOUND_INCOME_SOURCE" -> NotFoundError,
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
            val desResponse: DesResponse[SingleError] = DesResponse(correlationId, SingleError(Error(desErrorCode, "doesn't matter")))
            val expected: ErrorWrapper = ErrorWrapper(correlationId, mtdError, None)
            MockedDesConnector.amendAnnualSummary(request).returns(Future.successful(Left(desResponse)))
            val result: AmendSavingsAccountAnnualSummaryOutcome = await(service.amend(request))
            result shouldBe Left(expected)
          }
        }
    }
  }

  "retrieve" when {
    val request = RetrieveSavingsAccountAnnualSummaryRequest(Nino(nino), DesTaxYear.fromMtd(taxYear), incomeSourceId)
    "valid data is passed" should {
      "return a valid response" in new Test {
        val desResponse: DesResponse[DesRetrieveSavingsAccountAnnualIncomeResponse] =
          DesResponse(correlationId, DesRetrieveSavingsAccountAnnualIncomeResponse(Seq(
          DesSavingsInterestAnnualIncome(incomeSourceId, Some(2000.99), Some(5000.50))
        )))

        MockedDesConnector.retrieveAnnualSummary(request).returns(Future.successful(Right(desResponse)))

        val result: RetrieveSavingsAccountAnnualSummaryOutcome = await(service.retrieve(request))
        result shouldBe Right(DesResponse(correlationId, SavingsAccountAnnualSummary(Some(2000.99), Some(5000.50))))
      }
    }

    "no accounts are returned" should {
      "return 404 with error code MATCHING_RESOURCE_NOT_FOUND" in new Test {
        val expected: DesResponse[DesRetrieveSavingsAccountAnnualIncomeResponse] =
          DesResponse(correlationId, DesRetrieveSavingsAccountAnnualIncomeResponse(Nil))

        MockedDesConnector.retrieveAnnualSummary(request).returns(Future.successful(Right(expected)))

        val result: RetrieveSavingsAccountAnnualSummaryOutcome = await(service.retrieve(request))
        result shouldBe Left(ErrorWrapper(correlationId, NotFoundError, None))
      }
    }

    "multiple accounts are returned" should {
      "return error" in new Test {
        val expected: DesResponse[DesRetrieveSavingsAccountAnnualIncomeResponse] = DesResponse(correlationId, DesRetrieveSavingsAccountAnnualIncomeResponse(Seq(
          DesSavingsInterestAnnualIncome(incomeSourceId, Some(2000.99), Some(5000.50)),
          DesSavingsInterestAnnualIncome(incomeSourceId, Some(123.45), Some(543.21))
        )))

        MockedDesConnector.retrieveAnnualSummary(request).returns(Future.successful(Right(expected)))

        val result: RetrieveSavingsAccountAnnualSummaryOutcome = await(service.retrieve(request))
        result shouldBe Left(ErrorWrapper(correlationId, DownstreamError, None))
      }
    }

    "DES returns multiple errors" should {
      "return multiple errors" in new Test {
        val desResponse: DesResponse[MultipleErrors] = DesResponse(correlationId, MultipleErrors(Seq(desNinoFormatError, deaTaxYearFormatError)))

        MockedDesConnector.retrieveAnnualSummary(request).returns(Future.successful(Left(desResponse)))

        val result: RetrieveSavingsAccountAnnualSummaryOutcome = await(service.retrieve(request))
        result shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }
    }

    "the connector returns an outbound error" should {
      "return that outbound error as-is" in new Test {
        val fakeError: Error = Error("doesn't matter", "really doesn't matter")
        val desResponse: DesResponse[OutboundError] = DesResponse(correlationId, OutboundError(fakeError))

        MockedDesConnector.retrieveAnnualSummary(request).returns(Future.successful(Left(desResponse)))

        val result: RetrieveSavingsAccountAnnualSummaryOutcome = await(service.retrieve(request))
        result shouldBe Left(ErrorWrapper(correlationId, fakeError, None))
      }
    }

    "one of the errors from DES is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected: DesResponse[MultipleErrors] = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, DownstreamError)))
        MockedDesConnector.retrieveAnnualSummary(request).returns(Future.successful(Left(expected)))
        val result: RetrieveSavingsAccountAnnualSummaryOutcome = await(service.retrieve(request))
        result shouldBe Left(ErrorWrapper(correlationId, DownstreamError, None))
      }
    }

    Map(
      "INVALID_TYPE" -> DownstreamError,
      "INVALID_NINO" -> NinoFormatError,
      "INVALID_TAXYEAR" -> TaxYearFormatError,
      "INVALID_INCOME_SOURCE" -> AccountIdFormatError,
      "NOT_FOUND_PERIOD" -> NotFoundError,
      "NOT_FOUND_INCOME_SOURCE" -> NotFoundError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError
    ).foreach {
      case (desErrorCode, mtdError) =>
        s"DES returns a $desErrorCode error" should {
          s"return a ${mtdError.code} error" in new Test {
            val desResponse: DesResponse[SingleError] = DesResponse(correlationId, SingleError(Error(desErrorCode, "doesn't matter")))
            val expected: ErrorWrapper = ErrorWrapper(correlationId, mtdError, None)
            MockedDesConnector.retrieveAnnualSummary(request).returns(Future.successful(Left(desResponse)))
            val result: RetrieveSavingsAccountAnnualSummaryOutcome = await(service.retrieve(request))
            result shouldBe Left(expected)
          }
        }
    }
  }

}
