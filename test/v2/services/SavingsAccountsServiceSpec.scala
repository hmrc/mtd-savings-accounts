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
import v2.models.domain.{CreateSavingsAccountRequest, CreateSavingsAccountResponse, RetrieveAllSavingsAccountResponse, RetrieveSavingsAccountResponse}
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData.{CreateSavingsAccountRequestData, RetrieveAllSavingsAccountRequest, RetrieveSavingsAccountRequest}

import scala.concurrent.Future

class SavingsAccountsServiceSpec extends ServiceSpec {

  val incomeSourceId = "ZZIS12345678901"
  val correlationId = "X-123"
  val nino = "AA123456A"
  val ninoForInvalidCases = "AA123456D"
  val accountName = "Main account name"
  val duplicateAccountName = "Main account name dupe"

  val maxAccountsReachedError = MtdError("MAX_ACCOUNTS_REACHED", "doesn't matter")
  val alreadyExistsError = MtdError("ALREADY_EXISTS", "doesn't matter")
  val serviceUnavailableError = MtdError("SERVICE_UNAVAILABLE", "doesn't matter")
  val ninoFormatError = MtdError("INVALID_IDVALUE", "doesn't matter")
  val matchingResourceNotFoundError = MtdError("NOT_FOUND", "doesn't matter")

  trait Test extends MockDesConnector {
    lazy val service = new SavingsAccountsService(connector)
  }

  "create" when {
    val validRequest = CreateSavingsAccountRequestData(Nino(nino), CreateSavingsAccountRequest(accountName))
    val invalidRequest = CreateSavingsAccountRequestData(Nino(nino), CreateSavingsAccountRequest(duplicateAccountName))
    "valid data is passed" should {
      "return a valid id" in new Test {
        val expected = DesResponse(correlationId, CreateSavingsAccountResponse(incomeSourceId))
        MockedDesConnector.create(validRequest).returns(Future.successful(Right(expected)))
        val result: CreateSavingsAccountOutcome = await(service.create(validRequest))
        result shouldBe Right(expected)
      }
    }

    "DES returns multiple errors" should {
      "return multiple errors" in new Test {
        val expected = DesResponse(correlationId, MultipleErrors(Seq(AccountNameDuplicateError, MaximumSavingsAccountsLimitError)))
        val desResponse = DesResponse(correlationId, MultipleErrors(Seq(alreadyExistsError, maxAccountsReachedError)))
        MockedDesConnector.create(invalidRequest).returns(Future.successful(Left(desResponse)))
        val result: CreateSavingsAccountOutcome = await(service.create(invalidRequest))
        result shouldBe Left(ErrorWrapper(Some(correlationId), BadRequestError, Some(expected.responseData.errors)))
      }
    }

    "the connector returns an outbound error" should {
      "return that outbound error as-is" in new Test {
        val fakeError = MtdError("doesn't matter", "really doesn't matter")
        val desResponse = DesResponse(correlationId, OutboundError(fakeError))
        val expected = DesResponse(correlationId, OutboundError(fakeError))
        MockedDesConnector.create(invalidRequest).returns(Future.successful(Left(desResponse)))
        val result: CreateSavingsAccountOutcome = await(service.create(invalidRequest))
        result shouldBe Left(ErrorWrapper(Some(correlationId), expected.responseData.error, None))
      }
    }

    "one of the errors from DES is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected = DesResponse(correlationId, MultipleErrors(Seq(alreadyExistsError, serviceUnavailableError)))
        MockedDesConnector.create(invalidRequest).returns(Future.successful(Left(expected)))
        val result: CreateSavingsAccountOutcome = await(service.create(invalidRequest))
        result shouldBe Left(ErrorWrapper(Some(correlationId), DownstreamError, None))
      }
    }

    Map(
      "INVALID_IDTYPE" -> DownstreamError,
      "INVALID_IDVALUE" -> NinoFormatError,
      "MAX_ACCOUNTS_REACHED" -> MaximumSavingsAccountsLimitError,
      "ALREADY_EXISTS" -> AccountNameDuplicateError,
      "INVALID_PAYLOAD" -> BadRequestError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError,
      "UNEXPECTED_ERROR" -> DownstreamError
    ).foreach {
      case (k, v) =>
        s"DES returns a $k error" should {
          s"return a ${v.code} error" in new Test {
            val input = CreateSavingsAccountRequestData(Nino(nino), CreateSavingsAccountRequest("doesn't matter"))
            val desResponse = DesResponse(correlationId, SingleError(MtdError(k, "doesn't matter")))
            val expected = ErrorWrapper(Some(correlationId), v, None)
            MockedDesConnector.create(input).returns(Future.successful(Left(desResponse)))
            val result = await(service.create(input))
            result shouldBe Left(expected)
          }
        }
    }
  }

  "retrieveAll" when {
    val modelList = List(RetrieveAllSavingsAccountResponse(incomeSourceId, accountName))
    val validRequest = RetrieveAllSavingsAccountRequest(Nino(nino))
    val invalidRequest = RetrieveAllSavingsAccountRequest(Nino(ninoForInvalidCases))
    "valid data is passed" should {
      "return a valid response" in new Test {
        val expected = DesResponse(correlationId, modelList)
        MockedDesConnector.retrieveAll(validRequest).returns(Future.successful(Right(expected)))
        val result: RetrieveAllSavingsAccountsOutcome = await(service.retrieveAll(validRequest))
        result shouldBe Right(expected)
      }
    }

    "DES returns multiple errors" should {
      "return multiple errors" in new Test {
        val expected = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, NotFoundError)))
        val desResponse = DesResponse(correlationId, MultipleErrors(Seq(ninoFormatError, matchingResourceNotFoundError)))
        MockedDesConnector.retrieveAll(invalidRequest).returns(Future.successful(Left(desResponse)))
        val result: RetrieveAllSavingsAccountsOutcome = await(service.retrieveAll(invalidRequest))
        result shouldBe Left(ErrorWrapper(Some(correlationId), BadRequestError, Some(expected.responseData.errors)))
      }
    }

    "the connector returns an outbound error" should {
      "return that outbound error as-is" in new Test {
        val fakeError = MtdError("doesn't matter", "really doesn't matter")
        val desResponse = DesResponse(correlationId, OutboundError(fakeError))
        val expected = DesResponse(correlationId, OutboundError(fakeError))
        MockedDesConnector.retrieveAll(invalidRequest).returns(Future.successful(Left(desResponse)))
        val result: RetrieveAllSavingsAccountsOutcome = await(service.retrieveAll(invalidRequest))
        result shouldBe Left(ErrorWrapper(Some(correlationId), expected.responseData.error, None))
      }
    }

    "one of the errors from DES is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val expected = DesResponse(correlationId, MultipleErrors(Seq(ninoFormatError, serviceUnavailableError)))
        MockedDesConnector.retrieveAll(invalidRequest).returns(Future.successful(Left(expected)))
        val result: RetrieveAllSavingsAccountsOutcome = await(service.retrieveAll(invalidRequest))
        result shouldBe Left(ErrorWrapper(Some(correlationId), DownstreamError, None))
      }
    }

    Map(
      "INVALID_IDTYPE" -> DownstreamError,
      "INVALID_IDVALUE" -> NinoFormatError,
      "INVALID_INCOMESOURCETYPE" -> DownstreamError,
      "INVALID_TAXYEAR" -> DownstreamError,
      "INVALID_INCOMESOURCEID" -> DownstreamError,
      "INVALID_ENDDATE" -> DownstreamError,
      "NOT_FOUND" -> NotFoundError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError,
      "UNEXPECTED_ERROR" -> DownstreamError
    ).foreach {
      case (k, v) =>
        s"DES returns a $k error" should {
          s"return a ${v.code} error" in new Test {
            val input = RetrieveAllSavingsAccountRequest(Nino(ninoForInvalidCases))
            val desResponse = DesResponse(correlationId, SingleError(MtdError(k, "doesn't matter")))
            val expected = ErrorWrapper(Some(correlationId), v, None)
            MockedDesConnector.retrieveAll(input).returns(Future.successful(Left(desResponse)))
            val result = await(service.retrieveAll(input))
            result shouldBe Left(expected)
          }
        }
    }
  }

  "retrieve" when {
    val account = RetrieveSavingsAccountResponse(accountName)
    val validRequest = RetrieveSavingsAccountRequest(Nino(nino), incomeSourceId)
    val invalidRequest = RetrieveSavingsAccountRequest(Nino(ninoForInvalidCases), incomeSourceId)
    "valid data is passed" should {
      "return a valid response" in new Test {
        MockedDesConnector.retrieve(validRequest).returns(Future.successful(Right(DesResponse(correlationId, List(account)))))
        val result: RetrieveSavingsAccountsOutcome = await(service.retrieve(validRequest))
        result shouldBe Right(DesResponse(correlationId, account))
      }
    }

    "no accounts are returned" should {
      "return 404 with error code MATCHING_RESOURCE_NOT_FOUND" in new Test {
        MockedDesConnector.retrieve(validRequest).returns(Future.successful(Right(DesResponse(correlationId, Nil))))
        val result: RetrieveSavingsAccountsOutcome = await(service.retrieve(validRequest))
        result shouldBe Left(ErrorWrapper(Some(correlationId), NotFoundError, None))
      }
    }


    "multiple accounts are returned" should {
      "return error" in new Test {
        val account1 = RetrieveSavingsAccountResponse("account1")
        val account2 = RetrieveSavingsAccountResponse("account2")
        MockedDesConnector.retrieve(validRequest).returns(Future.successful(Right(DesResponse(correlationId, List(account1, account2)))))
        val result: RetrieveSavingsAccountsOutcome = await(service.retrieve(validRequest))
        result shouldBe Left(ErrorWrapper(Some(correlationId), DownstreamError, None))
      }
    }

    "DES returns multiple errors" should {
      "return multiple errors" in new Test {
        val desResponse = DesResponse(correlationId, MultipleErrors(Seq(ninoFormatError, matchingResourceNotFoundError)))
        MockedDesConnector.retrieve(invalidRequest).returns(Future.successful(Left(desResponse)))
        val result: RetrieveSavingsAccountsOutcome = await(service.retrieve(invalidRequest))
        result shouldBe Left(ErrorWrapper(Some(correlationId), BadRequestError, Some(Seq(NinoFormatError, NotFoundError))))
      }
    }

    "the connector returns an outbound error" should {
      "return that outbound error as-is" in new Test {
        val fakeError = MtdError("doesn't matter", "really doesn't matter")
        val desResponse = DesResponse(correlationId, OutboundError(fakeError))
        MockedDesConnector.retrieve(invalidRequest).returns(Future.successful(Left(desResponse)))
        val result: RetrieveSavingsAccountsOutcome = await(service.retrieve(invalidRequest))
        result shouldBe Left(ErrorWrapper(Some(correlationId), fakeError, None))
      }
    }

    "one of the errors from DES is a DownstreamError" should {
      "return a single error if there are multiple errors" in new Test {
        val dnsResponse = DesResponse(correlationId, MultipleErrors(Seq(ninoFormatError, serviceUnavailableError)))
        MockedDesConnector.retrieve(invalidRequest).returns(Future.successful(Left(dnsResponse)))
        val result: RetrieveSavingsAccountsOutcome = await(service.retrieve(invalidRequest))
        result shouldBe Left(ErrorWrapper(Some(correlationId), DownstreamError, None))
      }
    }

    Map(
      "INVALID_IDTYPE" -> DownstreamError,
      "INVALID_IDVALUE" -> NinoFormatError,
      "INVALID_INCOMESOURCETYPE" -> DownstreamError,
      "INVALID_TAXYEAR" -> DownstreamError,
      "INVALID_INCOMESOURCEID" -> AccountIdFormatError,
      "INVALID_ENDDATE" -> DownstreamError,
      "NOT_FOUND" -> NotFoundError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError,
      "UNEXPECTED_ERROR" -> DownstreamError
    ).foreach {
      case (k, v) =>
        s"DES returns a $k error" should {
          s"return a ${v.code} error" in new Test {
            val input = RetrieveSavingsAccountRequest(Nino(ninoForInvalidCases), incomeSourceId)
            val desResponse = DesResponse(correlationId, SingleError(MtdError(k, "doesn't matter")))
            val expected = ErrorWrapper(Some(correlationId), v, None)
            MockedDesConnector.retrieve(input).returns(Future.successful(Left(desResponse)))
            val result = await(service.retrieve(input))
            result shouldBe Left(expected)
          }
        }
    }
  }

}
