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

package v2.connectors

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import v2.config.AppConfig
import v2.httpparsers.StandardDesHttpParser
import v2.models.requestData.{AmendSavingsAccountAnnualSummaryRequest, CreateSavingsAccountRequestData, RetrieveAllSavingsAccountRequest, RetrieveSavingsAccountRequest}
import v2.models.domain._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector @Inject()(http: HttpClient,
                             appConfig: AppConfig) {

  val logger = Logger(this.getClass)

  import v2.httpparsers.StandardDesHttpParser._

  private[connectors] def desHeaderCarrier(implicit hc: HeaderCarrier): HeaderCarrier = hc
    .copy(authorization = Some(Authorization(s"Bearer ${appConfig.desToken}")))
    .withExtraHeaders("Environment" -> appConfig.desEnv)

  def createSavingsAccount(createSavingsAccountRequestData: CreateSavingsAccountRequestData)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CreateSavingsAccountConnectorOutcome] = {

    import CreateSavingsAccountRequest.writes
    val nino = createSavingsAccountRequestData.nino.nino

    val url = s"${appConfig.desBaseUrl}/income-tax/income-sources/nino/$nino"

    http.POST[CreateSavingsAccountRequest,
      CreateSavingsAccountConnectorOutcome](url,
      createSavingsAccountRequestData.createSavingsAccount)(writes, reads[CreateSavingsAccountResponse], desHeaderCarrier, implicitly)

  }

  def retrieveAllSavingsAccounts(retrieveAllSavingsAccountRequest: RetrieveAllSavingsAccountRequest)
                                (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RetrieveAllSavingsAccountsConnectorOutcome] = {

    val nino = retrieveAllSavingsAccountRequest.nino.nino

    val url = s"${appConfig.desBaseUrl}/income-tax/income-sources/nino/$nino?incomeSourceType=interest-from-uk-banks"

    http.GET[RetrieveAllSavingsAccountsConnectorOutcome](url)(
      StandardDesHttpParser.reads[List[RetrieveAllSavingsAccountResponse]], desHeaderCarrier, implicitly)
  }

  def retrieveSavingsAccount(request: RetrieveSavingsAccountRequest)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DesConnectorOutcome[List[RetrieveSavingsAccountResponse]]] = {

    val nino = request.nino.nino
    val incomeSourceId = request.accountId

    val url = s"${appConfig.desBaseUrl}/income-tax/income-sources/nino/$nino?incomeSourceType=interest-from-uk-banks&incomeSourceId=$incomeSourceId"

    http.GET[DesConnectorOutcome[List[RetrieveSavingsAccountResponse]]](url)(
      reads[List[RetrieveSavingsAccountResponse]], desHeaderCarrier, implicitly)
  }

  def amendSavingsAccountAnnualSummary(amendSavingsAccountAnnualSummaryRequest: AmendSavingsAccountAnnualSummaryRequest)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AmendSavingsAccountAnnualSummaryConnectorOutcome] = {

    import v2.models.domain.SavingsAccountAnnualSummary.desWrites
    val nino = amendSavingsAccountAnnualSummaryRequest.nino.nino
    val desTaxYear = amendSavingsAccountAnnualSummaryRequest.desTaxYear
    val incomeSourceId = amendSavingsAccountAnnualSummaryRequest.savingsAccountId

    val url = s"${appConfig.desBaseUrl}/income-tax/nino/$nino/income-source/savings/annual/$desTaxYear"

    http.POST[SavingsAccountAnnualSummary,
      AmendSavingsAccountAnnualSummaryConnectorOutcome](url,
      amendSavingsAccountAnnualSummaryRequest.
        savingsAccountAnnualSummary)(desWrites(incomeSourceId), reads[AmendSavingsAccountAnnualSummaryResponse], desHeaderCarrier, implicitly)

  }
}
