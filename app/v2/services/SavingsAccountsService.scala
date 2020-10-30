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

package v2.services

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData.{CreateSavingsAccountRequestData, RetrieveAllSavingsAccountRequest, RetrieveSavingsAccountRequest}

import scala.concurrent.{ExecutionContext, Future}

class SavingsAccountsService @Inject()(connector: DesConnector) extends DesServiceSupport {

  override val serviceName: String = "SavingsAccountsService"

  def create(request: CreateSavingsAccountRequestData)
            (implicit hc: HeaderCarrier,
             ec: ExecutionContext,
             correlationId: String): Future[CreateSavingsAccountOutcome] = {
    connector.createSavingsAccount(request)
      .map(mapToVendorDirect("create", desErrorToMtdErrorCreate))
  }

  def retrieveAll(request: RetrieveAllSavingsAccountRequest)
                 (implicit hc: HeaderCarrier,
                  ec: ExecutionContext,
                  correlationId: String): Future[RetrieveAllSavingsAccountsOutcome] = {
    connector.retrieveAllSavingsAccounts(request)
      .map(mapToVendorDirect("retrieveAll", desErrorToMtdErrorRetrieveAll))
  }

  def retrieve(request: RetrieveSavingsAccountRequest)(implicit hc: HeaderCarrier,
                                                       ec: ExecutionContext,
                                                       correlationId: String): Future[RetrieveSavingsAccountsOutcome] = {
    connector.retrieveSavingsAccount(request).map {
      mapToVendor("retrieve", desErrorToMtdErrorRetrieve) {
        desResponse =>
          desResponse.responseData match {
            case ac :: Nil => Right(DesResponse(desResponse.correlationId, ac))
            case Nil       => Left(ErrorWrapper(desResponse.correlationId, NotFoundError, None))
            case _         =>
              logger.info(s"[SavingsAccountsService] [retrieve] [CorrelationId - ${desResponse.correlationId}] - " +
                "More than one matching account found")
              Left(ErrorWrapper(desResponse.correlationId, DownstreamError, None))
          }
      }
    }
  }

  private def desErrorToMtdErrorCreate: Map[String, Error] = Map(
    "INVALID_IDTYPE" -> DownstreamError,
    "INVALID_IDVALUE" -> NinoFormatError,
    "MAX_ACCOUNTS_REACHED" -> MaximumSavingsAccountsLimitError,
    "ALREADY_EXISTS" -> AccountNameDuplicateError,
    "INVALID_PAYLOAD" -> BadRequestError,
    "SERVER_ERROR" -> DownstreamError,
    "SERVICE_UNAVAILABLE" -> DownstreamError
  ).withDefault { error =>
    logger.info(s"[SavingsAccountsService] [create] - No mapping found for error code $error")
    DownstreamError
  }


  private def desErrorToMtdErrorRetrieveAll: Map[String, Error] = Map(
    "INVALID_IDTYPE" -> DownstreamError,
    "INVALID_IDVALUE" -> NinoFormatError,
    "INVALID_INCOMESOURCETYPE" -> DownstreamError,
    "INVALID_TAXYEAR" -> DownstreamError,
    "INVALID_INCOMESOURCEID" -> DownstreamError,
    "INVALID_ENDDATE" -> DownstreamError,
    "NOT_FOUND" -> NotFoundError,
    "SERVER_ERROR" -> DownstreamError,
    "SERVICE_UNAVAILABLE" -> DownstreamError
  ).withDefault { error =>
    logger.info(s"[SavingsAccountsService] [retrieveAll] - No mapping found for error code $error")
    DownstreamError
  }

  private def desErrorToMtdErrorRetrieve: Map[String, Error] = Map(
    "INVALID_IDTYPE" -> DownstreamError,
    "INVALID_IDVALUE" -> NinoFormatError,
    "INVALID_INCOMESOURCETYPE" -> DownstreamError,
    "INVALID_TAXYEAR" -> DownstreamError,
    "INVALID_INCOMESOURCEID" -> AccountIdFormatError,
    "INVALID_ENDDATE" -> DownstreamError,
    "NOT_FOUND" -> NotFoundError,
    "SERVER_ERROR" -> DownstreamError,
    "SERVICE_UNAVAILABLE" -> DownstreamError
  ).withDefault { error =>
    logger.info(s"[SavingsAccountsService] [retrieve] - No mapping found for error code $error")
    DownstreamError
  }

}
