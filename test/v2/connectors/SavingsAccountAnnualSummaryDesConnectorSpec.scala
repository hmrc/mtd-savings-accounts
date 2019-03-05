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

import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import v2.mocks.{MockAppConfig, MockHttpClient}
import v2.models.des.{DesAmendSavingsAccountAnnualSummaryResponse, DesRetrieveSavingsAccountAnnualIncomeResponse, DesSavingsInterestAnnualIncome}
import v2.models.domain._
import v2.models.errors.{MultipleErrors, NinoFormatError, SingleError, TaxYearFormatError}
import v2.models.outcomes.DesResponse
import v2.models.requestData.{AmendSavingsAccountAnnualSummaryRequest, DesTaxYear, RetrieveSavingsAccountAnnualSummaryRequest}

import scala.concurrent.Future

class SavingsAccountAnnualSummaryDesConnectorSpec extends ConnectorSpec {

  lazy val baseUrl = "test-BaseUrl"

  val incomeSourceId = "ZZIS12345678901"
  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino = "AA123456A"
  val taxYear = "2018-19"
  val transactionReference = "0000000000000001"


  val desRequestBody = Json.parse(
    """
      |{
      |  "incomeSourceId": "ZZIS12345678901",
      |  "taxedUKInterest": 2000.99,
      |  "untaxedUKInterest": 5000.50
      |}
    """.stripMargin)

  class Test extends MockHttpClient with MockAppConfig {
    val connector = new DesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )
    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
  }

  val savingsAccountAnnualSummary = SavingsAccountAnnualSummary(Some(2000.99), Some(5000.50))
  val desSavingsAccountAnnualIncome = DesSavingsInterestAnnualIncome(incomeSourceId, Some(2000.99), Some(5000.50))

  "Amend annual summary" when {
    val request = AmendSavingsAccountAnnualSummaryRequest(Nino(nino), DesTaxYear.fromMtd(taxYear), incomeSourceId, savingsAccountAnnualSummary)
    "a valid request is supplied" should {
      "return a successful response with correlationId" in new Test {

        val expectedDesResponse = DesResponse(correlationId, DesAmendSavingsAccountAnnualSummaryResponse(transactionReference))

        MockedHttpClient.post[DesSavingsInterestAnnualIncome, AmendSavingsAccountAnnualSummaryConnectorOutcome](
          s"$baseUrl" + s"/income-tax/nino/$nino/income-source/savings/annual/${DesTaxYear.fromMtd(taxYear)}",
          desSavingsAccountAnnualIncome
        ).returns(Future.successful(Right(expectedDesResponse)))

        val result: AmendSavingsAccountAnnualSummaryConnectorOutcome =
          await(connector.amendSavingsAccountAnnualSummary(request))

        result shouldBe Right(expectedDesResponse)
      }
    }

    "invalid request is supplied" should {
      "return an error response with a single error and the correlationId" in new Test {

        val expectedDesResponse = DesResponse(correlationId, SingleError(NinoFormatError))

        MockedHttpClient.post[DesSavingsInterestAnnualIncome, AmendSavingsAccountAnnualSummaryConnectorOutcome](
          s"$baseUrl" + s"/income-tax/nino/$nino/income-source/savings/annual/${DesTaxYear.fromMtd(taxYear)}",
          desSavingsAccountAnnualIncome
        ).returns(Future.successful(Left(expectedDesResponse)))

        val result: AmendSavingsAccountAnnualSummaryConnectorOutcome =
          await(connector.amendSavingsAccountAnnualSummary(request))

        result shouldBe Left(expectedDesResponse)
      }
    }

    "invalid request is supplied" should {
      "return an error response with multiple errors and the correlationId" in new Test {

        val expectedDesResponse = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError)))

        MockedHttpClient.post[DesSavingsInterestAnnualIncome, AmendSavingsAccountAnnualSummaryConnectorOutcome](
          s"$baseUrl" + s"/income-tax/nino/$nino/income-source/savings/annual/${DesTaxYear.fromMtd(taxYear)}",
          desSavingsAccountAnnualIncome
        ).returns(Future.successful(Left(expectedDesResponse)))

        val result: AmendSavingsAccountAnnualSummaryConnectorOutcome =
          await(connector.amendSavingsAccountAnnualSummary(request))

        result shouldBe Left(expectedDesResponse)
      }
    }
  }

  "Retrieve annual summary" when {
    val request = RetrieveSavingsAccountAnnualSummaryRequest(Nino(nino), DesTaxYear.fromMtd(taxYear), incomeSourceId)

    "a valid request is supplied" should {
      "return a successful response with correlationId" in new Test {
        val expectedDesResponse = DesResponse(correlationId, DesRetrieveSavingsAccountAnnualIncomeResponse(
          Seq(DesSavingsInterestAnnualIncome(incomeSourceId, Some(2000.99), Some(5000.50)))
        ))

        MockedHttpClient.get[RetrieveSavingsAccountAnnualSummaryConnectorOutcome](
          s"$baseUrl" + s"/income-tax/nino/$nino/income-source/savings/annual/${DesTaxYear.fromMtd(taxYear)}?incomeSourceId=$incomeSourceId"
        ).returns(Future.successful(Right(expectedDesResponse)))

        val result = await(connector.retrieveSavingsAccountAnnualSummary(request))

        result shouldBe Right(expectedDesResponse)
      }
    }

    "an invalid request is supplied" should {

      "return an error response with a single error and the correlationId" in new Test {

        val expectedDesResponse = DesResponse(correlationId, SingleError(NinoFormatError))

        MockedHttpClient.get[RetrieveSavingsAccountAnnualSummaryConnectorOutcome](
          s"$baseUrl" + s"/income-tax/nino/$nino/income-source/savings/annual/${DesTaxYear.fromMtd(taxYear)}?incomeSourceId=$incomeSourceId"
        ).returns(Future.successful(Left(expectedDesResponse)))

        val result= await(connector.retrieveSavingsAccountAnnualSummary(request))

        result shouldBe Left(expectedDesResponse)
      }

      "return an error response with multiple errors and the correlationId" in new Test {

        val expectedDesResponse = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError)))

        MockedHttpClient.get[RetrieveSavingsAccountAnnualSummaryConnectorOutcome](
          s"$baseUrl" + s"/income-tax/nino/$nino/income-source/savings/annual/${DesTaxYear.fromMtd(taxYear)}?incomeSourceId=$incomeSourceId"
        ).returns(Future.successful(Left(expectedDesResponse)))

        val result = await(connector.retrieveSavingsAccountAnnualSummary(request))

        result shouldBe Left(expectedDesResponse)
      }
    }
  }
}
