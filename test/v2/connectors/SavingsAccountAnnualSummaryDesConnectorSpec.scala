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

package v2.connectors

import play.api.libs.json.{JsValue, Json}
import v2.mocks.{MockAppConfig, MockHttpClient}
import v2.models.des.{DesAmendSavingsAccountAnnualSummaryResponse, DesRetrieveSavingsAccountAnnualIncomeResponse, DesSavingsInterestAnnualIncome}
import v2.models.domain.{Nino, _}
import v2.models.errors.{MultipleErrors, NinoFormatError, SingleError, TaxYearFormatError}
import v2.models.outcomes.DesResponse
import v2.models.requestData.{AmendSavingsAccountAnnualSummaryRequest, DesTaxYear, RetrieveSavingsAccountAnnualSummaryRequest}

import scala.concurrent.Future

class SavingsAccountAnnualSummaryDesConnectorSpec extends ConnectorSpec {

  val incomeSourceId = "ZZIS12345678901"
  val nino = "AA123456A"
  val taxYear = "2018-19"
  val transactionReference = "0000000000000001"


  val desRequestBody: JsValue = Json.parse(
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
    MockAppConfig.desBaseUrl returns baseUrl
    MockAppConfig.desToken returns "des-token"
    MockAppConfig.desEnvironment returns "des-environment"
    MockAppConfig.desEnvironmentHeaders returns Some(allowedDesHeaders)
  }

  val savingsAccountAnnualSummary: SavingsAccountAnnualSummary = SavingsAccountAnnualSummary(Some(2000.99), Some(5000.50))
  val desSavingsAccountAnnualIncome: DesSavingsInterestAnnualIncome = DesSavingsInterestAnnualIncome(incomeSourceId, Some(2000.99), Some(5000.50))

  "Amend annual summary" when {
    val request = AmendSavingsAccountAnnualSummaryRequest(Nino(nino), DesTaxYear.fromMtd(taxYear), incomeSourceId, savingsAccountAnnualSummary)
    "a valid request is supplied" should {
      "return a successful response with correlationId" in new Test {

        val expectedDesResponse: DesResponse[DesAmendSavingsAccountAnnualSummaryResponse] =
          DesResponse(correlationId, DesAmendSavingsAccountAnnualSummaryResponse(transactionReference))

        MockedHttpClient.post[DesSavingsInterestAnnualIncome, AmendSavingsAccountAnnualSummaryConnectorOutcome](
          url = s"$baseUrl" + s"/income-tax/nino/$nino/income-source/savings/annual/${DesTaxYear.fromMtd(taxYear)}",
          config = dummyDesHeaderCarrierConfig,
          body = desSavingsAccountAnnualIncome,
          requiredHeaders = requiredDesHeaders,
          excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
        ).returns(Future.successful(Right(expectedDesResponse)))

        val result: AmendSavingsAccountAnnualSummaryConnectorOutcome =
          await(connector.amendSavingsAccountAnnualSummary(request))

        result shouldBe Right(expectedDesResponse)
      }
    }

    "invalid request is supplied" should {
      "return an error response with a single error and the correlationId" in new Test {

        val expectedDesResponse: DesResponse[SingleError] = DesResponse(correlationId, SingleError(NinoFormatError))

        MockedHttpClient.post[DesSavingsInterestAnnualIncome, AmendSavingsAccountAnnualSummaryConnectorOutcome](
          url = s"$baseUrl" + s"/income-tax/nino/$nino/income-source/savings/annual/${DesTaxYear.fromMtd(taxYear)}",
          config = dummyDesHeaderCarrierConfig,
          body = desSavingsAccountAnnualIncome,
          requiredHeaders = requiredDesHeaders,
          excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
        ).returns(Future.successful(Left(expectedDesResponse)))

        val result: AmendSavingsAccountAnnualSummaryConnectorOutcome =
          await(connector.amendSavingsAccountAnnualSummary(request))

        result shouldBe Left(expectedDesResponse)
      }
    }

    "invalid request is supplied" should {
      "return an error response with multiple errors and the correlationId" in new Test {

        val expectedDesResponse: DesResponse[MultipleErrors] = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError)))

        MockedHttpClient.post[DesSavingsInterestAnnualIncome, AmendSavingsAccountAnnualSummaryConnectorOutcome](
          url = s"$baseUrl" + s"/income-tax/nino/$nino/income-source/savings/annual/${DesTaxYear.fromMtd(taxYear)}",
          config = dummyDesHeaderCarrierConfig,
          body = desSavingsAccountAnnualIncome,
          requiredHeaders = requiredDesHeaders,
          excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
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
        val expectedDesResponse: DesResponse[DesRetrieveSavingsAccountAnnualIncomeResponse] =
          DesResponse(correlationId, DesRetrieveSavingsAccountAnnualIncomeResponse(
          Seq(DesSavingsInterestAnnualIncome(incomeSourceId, Some(2000.99), Some(5000.50)))
        ))

        MockedHttpClient.get[RetrieveSavingsAccountAnnualSummaryConnectorOutcome](
          url = s"$baseUrl" + s"/income-tax/nino/$nino/income-source/savings/annual/${DesTaxYear.fromMtd(taxYear)}?incomeSourceId=$incomeSourceId",
          config = dummyDesHeaderCarrierConfig,
          requiredHeaders = requiredDesHeaders,
          excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
        ).returns(Future.successful(Right(expectedDesResponse)))

        val result: RetrieveSavingsAccountAnnualSummaryConnectorOutcome = await(connector.retrieveSavingsAccountAnnualSummary(request))

        result shouldBe Right(expectedDesResponse)
      }
    }

    "an invalid request is supplied" should {

      "return an error response with a single error and the correlationId" in new Test {

        val expectedDesResponse: DesResponse[SingleError] = DesResponse(correlationId, SingleError(NinoFormatError))

        MockedHttpClient.get[RetrieveSavingsAccountAnnualSummaryConnectorOutcome](
          url = s"$baseUrl" + s"/income-tax/nino/$nino/income-source/savings/annual/${DesTaxYear.fromMtd(taxYear)}?incomeSourceId=$incomeSourceId",
          config = dummyDesHeaderCarrierConfig,
          requiredHeaders = requiredDesHeaders,
          excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
        ).returns(Future.successful(Left(expectedDesResponse)))

        val result: RetrieveSavingsAccountAnnualSummaryConnectorOutcome = await(connector.retrieveSavingsAccountAnnualSummary(request))

        result shouldBe Left(expectedDesResponse)
      }

      "return an error response with multiple errors and the correlationId" in new Test {

        val expectedDesResponse: DesResponse[MultipleErrors] = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError)))

        MockedHttpClient.get[RetrieveSavingsAccountAnnualSummaryConnectorOutcome](
          url = s"$baseUrl" + s"/income-tax/nino/$nino/income-source/savings/annual/${DesTaxYear.fromMtd(taxYear)}?incomeSourceId=$incomeSourceId",
          config = dummyDesHeaderCarrierConfig,
          requiredHeaders = requiredDesHeaders,
          excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
        ).returns(Future.successful(Left(expectedDesResponse)))

        val result: RetrieveSavingsAccountAnnualSummaryConnectorOutcome = await(connector.retrieveSavingsAccountAnnualSummary(request))

        result shouldBe Left(expectedDesResponse)
      }
    }
  }
}
