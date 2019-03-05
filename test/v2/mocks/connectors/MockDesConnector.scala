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

package v2.mocks.connectors

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors._
import v2.models.requestData._
import v2.services.AmendSavingsAccountAnnualSummaryOutcome

import scala.concurrent.{ExecutionContext, Future}

trait MockDesConnector extends MockFactory {

  val connector: DesConnector = mock[DesConnector]

  object MockedDesConnector {
    def create(createSavingsAccountRequestData: CreateSavingsAccountRequestData): CallHandler[Future[CreateSavingsAccountConnectorOutcome]] = {
      (connector.createSavingsAccount(_: CreateSavingsAccountRequestData)(_: HeaderCarrier, _: ExecutionContext))
        .expects(createSavingsAccountRequestData, *, *)
    }

    def retrieveAll(retrieveSavingsAccountRequest: RetrieveAllSavingsAccountRequest): CallHandler[Future[RetrieveAllSavingsAccountsConnectorOutcome]] = {
      (connector.retrieveAllSavingsAccounts(_: RetrieveAllSavingsAccountRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(retrieveSavingsAccountRequest, *, *)
    }

    def retrieve(retrieveSavingsAccountRequest: RetrieveSavingsAccountRequest): CallHandler[Future[RetrieveSavingsAccountConnectorOutcome]] = {
      (connector.retrieveSavingsAccount(_: RetrieveSavingsAccountRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(retrieveSavingsAccountRequest, *, *)
    }

    def amendAnnualSummary(amendSavingsAccountAnnualSummaryRequest:
                           AmendSavingsAccountAnnualSummaryRequest): CallHandler[Future[AmendSavingsAccountAnnualSummaryConnectorOutcome]] = {
      (connector.amendSavingsAccountAnnualSummary(_: AmendSavingsAccountAnnualSummaryRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(amendSavingsAccountAnnualSummaryRequest, *, *)
    }

    def retrieveAnnualSummary(request: RetrieveSavingsAccountAnnualSummaryRequest): CallHandler[Future[RetrieveSavingsAccountAnnualSummaryConnectorOutcome]] = {
      (connector.retrieveSavingsAccountAnnualSummary(_: RetrieveSavingsAccountAnnualSummaryRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(request, *, *)
    }
  }

}
