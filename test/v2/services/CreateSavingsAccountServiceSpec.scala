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
import v2.models.domain.SavingsAccount
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData.CreateSavingsAccountRequestData

import scala.concurrent.Future

class CreateSavingsAccountServiceSpec extends ServiceSpec {

  val incomeSourceId = "ZZIS12345678901"
  val correlationId = "X-123"
  val nino = "AA123456A"
  val accountName = "Main account name"
  val duplicateAccountName = "Main account name dupe"

  val validRequest = CreateSavingsAccountRequestData(Nino(nino), SavingsAccount(accountName))
  val invalidRequest = CreateSavingsAccountRequestData(Nino(nino), SavingsAccount(duplicateAccountName))

  val maxAccountsReachedError = Error("MAX_ACCOUNTS_REACHED", "doesn't matter")
  val alreadyExistsError = Error("ALREADY_EXISTS", "doesn't matter")
  val serviceUnavailableError = Error("SERVICE_UNAVAILABLE", "doesn't matter")

  trait Test extends MockDesConnector {
    lazy val service = new CreateSavingsAccountService(connector)
  }

  "create" when {
    "valid data is passed" should {
      "return a valid id" in new Test {
        val expected = DesResponse(correlationId, incomeSourceId)
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

    "DES returns an outbound error" should {
      "return that outbound error as-is" in new Test {
        val fakeError = Error("doesn't matter", "really doesn't matter")
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
      "SERVICE_UNAVAILABLE" -> DownstreamError
    ).foreach {
      case(k, v) =>
        s"DES returns a $k error" should {
          s"return a ${v.code} error" in new Test {
            val input = CreateSavingsAccountRequestData(Nino(nino), SavingsAccount("doesn't matter"))
            val desResponse = DesResponse(correlationId, SingleError(Error(k, "doesn't matter")))
            val expected = ErrorWrapper(Some(correlationId), v, None)
            MockedDesConnector.create(input).returns(Future.successful(Left(desResponse)))
            val result = await(service.create(input))
            result shouldBe Left(expected)
          }
        }
    }
  }

}
