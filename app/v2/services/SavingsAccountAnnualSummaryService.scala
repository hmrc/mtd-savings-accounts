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

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.models.errors._
import v2.models.requestData.AmendSavingsAccountAnnualSummaryRequest

import scala.concurrent.{ExecutionContext, Future}

class SavingsAccountAnnualSummaryService @Inject()(connector: DesConnector) extends DesServiceSupport {

  override val serviceName: String = "SavingsAccountsService"

  def amend(request: AmendSavingsAccountAnnualSummaryRequest)
            (implicit hc: HeaderCarrier,
             ec: ExecutionContext): Future[AmendSavingsAccountAnnualSummaryOutcome] = {
    connector.amendSavingsAccountAnnualSummary(request)
      .map(mapToVendorDirect("AMEND", desErrorToMtdErrorAmend))
  }

  private def desErrorToMtdErrorAmend: Map[String, Error] = Map(
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
    "SERVICE_UNAVAILABLE" -> DownstreamError
  ).withDefault { error =>
    logger.info(s"[SavingsAccountAnnualSummaryService] [amend] - No mapping found for error code $error")
    DownstreamError
  }
}
