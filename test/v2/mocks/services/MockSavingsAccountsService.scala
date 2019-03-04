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

package v2.mocks.services

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import v2.models.requestData._
import v2.services._

import scala.concurrent.{ExecutionContext, Future}

trait MockSavingsAccountsService extends MockFactory {

  val mockSavingsAccountService: SavingsAccountsService = mock[SavingsAccountsService]

  object MockSavingsAccountService {
    def create(savingsAccountRequest: CreateSavingsAccountRequestData): CallHandler[Future[CreateSavingsAccountOutcome]] = {
      (mockSavingsAccountService.create(_: CreateSavingsAccountRequestData)(_: HeaderCarrier, _: ExecutionContext))
        .expects(savingsAccountRequest, *, *)
    }

    def retrieveAll(savingsAccountRequest: RetrieveAllSavingsAccountRequest): CallHandler[Future[RetrieveAllSavingsAccountsOutcome]] = {
      (mockSavingsAccountService.retrieveAll(_: RetrieveAllSavingsAccountRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(savingsAccountRequest, *, *)
    }


    def retrieve(savingsAccountRequest: RetrieveSavingsAccountRequest): CallHandler[Future[RetrieveSavingsAccountsOutcome]] = {
      (mockSavingsAccountService.retrieve(_: RetrieveSavingsAccountRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(savingsAccountRequest, *, *)
    }
  }

}
