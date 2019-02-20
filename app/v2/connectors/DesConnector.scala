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
import v2.models.requestData.{CreateSavingsAccountRequestData, RetrieveSavingsAccountRequest}
import v2.models.domain.CreateSavingsAccount

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector @Inject()(http: HttpClient,
                             appConfig: AppConfig) {

  val logger = Logger(this.getClass)

  private[connectors] def desHeaderCarrier(implicit hc: HeaderCarrier): HeaderCarrier = hc
    .copy(authorization = Some(Authorization(s"Bearer ${appConfig.desToken}")))
    .withExtraHeaders("Environment" -> appConfig.desEnv)

  def create(createSavingsAccountRequestData: CreateSavingsAccountRequestData)
            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CreateSavingsAccountConnectorOutcome] = {

    import v2.connectors.httpparsers.CreateSavingsAccountHttpParser.createHttpReads
    import CreateSavingsAccount.writes

    val nino = createSavingsAccountRequestData.nino.nino

    val url = s"${appConfig.desBaseUrl}/income-tax/income-sources/nino/$nino"

    http.POST[CreateSavingsAccount, CreateSavingsAccountConnectorOutcome](url, createSavingsAccountRequestData.createSavingsAccount)(writes, createHttpReads,
      desHeaderCarrier, implicitly)

  }

  def retrieveAll(retrieveSavingsAccountRequest: RetrieveSavingsAccountRequest)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RetrieveAllSavingsAccountsConnectorOutcome] = {

    import v2.connectors.httpparsers.RetrieveAllSavingsAccountsHttpParser.retrieveHttpReads

    val nino = retrieveSavingsAccountRequest.nino.nino

    val url = s"${appConfig.desBaseUrl}/income-tax/income-sources/nino/$nino?incomeSourceType=interest-from-uk-banks"

    http.GET[RetrieveAllSavingsAccountsConnectorOutcome](url)(retrieveHttpReads, desHeaderCarrier, implicitly)
  }

}
