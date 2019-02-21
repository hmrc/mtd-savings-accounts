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

import uk.gov.hmrc.domain.Nino
import v2.mocks.{MockAppConfig, MockHttpClient}
import v2.models.domain.{CreateSavingsAccount, RetrieveAllSavingsAccount, RetrieveSavingsAccount}
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData.{CreateSavingsAccountRequestData, RetrieveAllSavingsAccountRequest, RetrieveSavingsAccountRequest}

import scala.concurrent.Future

class DesConnectorSpec extends ConnectorSpec {

  lazy val baseUrl = "test-BaseUrl"

  val incomeSourceId = "ZZIS12345678901"
  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val nino = "AA123456A"
  val accountName = "Main account name"
  val duplicateAccountName = "Main account name dupe"

  class Test extends MockHttpClient with MockAppConfig {
    val connector = new DesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )
    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
  }

  "Create income source" when {
    "a valid request is supplied" should {
      "return a successful response with incomeSourceId and the correct correlationId" in new Test {

        val expectedDesResponse = DesResponse(correlationId, incomeSourceId)

        MockedHttpClient.post[CreateSavingsAccount, CreateSavingsAccountConnectorOutcome](
          s"$baseUrl" + s"/income-tax/income-sources/nino/$nino",
          CreateSavingsAccount(accountName)
        ).returns(Future.successful(Right(expectedDesResponse)))

        val result: CreateSavingsAccountConnectorOutcome =
          await(connector.create(CreateSavingsAccountRequestData(Nino(nino), CreateSavingsAccount(accountName))))

        result shouldBe Right(expectedDesResponse)
      }
    }

    "a request containing an invalid account name is supplied" should {
      "return an error response with a single error and the correct correlationId" in new Test {

        val expectedDesResponse = DesResponse(correlationId, SingleError(AccountNameDuplicateError))

        MockedHttpClient.post[CreateSavingsAccount, CreateSavingsAccountConnectorOutcome](
          s"$baseUrl" + s"/income-tax/income-sources/nino/$nino",
          CreateSavingsAccount(duplicateAccountName)
        ).returns(Future.successful(Left(expectedDesResponse)))

        val result: CreateSavingsAccountConnectorOutcome =
          await(connector.create(CreateSavingsAccountRequestData(Nino(nino), CreateSavingsAccount(duplicateAccountName))))

        result shouldBe Left(expectedDesResponse)
      }
    }

    "a request containing an invalid account name is supplied and the user has reached their maximum number of savings accounts" should {
      "return an error response with multiple errors and the correct correlationId" in new Test {

        val expectedDesResponse = DesResponse(correlationId, MultipleErrors(Seq(AccountNameDuplicateError, MaximumSavingsAccountsLimitError)))

        MockedHttpClient.post[CreateSavingsAccount, CreateSavingsAccountConnectorOutcome](
          s"$baseUrl" + s"/income-tax/income-sources/nino/$nino",
          CreateSavingsAccount(duplicateAccountName)
        ).returns(Future.successful(Left(expectedDesResponse)))

        val result: CreateSavingsAccountConnectorOutcome =
          await(connector.create(CreateSavingsAccountRequestData(Nino(nino), CreateSavingsAccount(duplicateAccountName))))

        result shouldBe Left(expectedDesResponse)
      }
    }
  }

  "Retrieve all savings accounts" when {
    val expectedResponseBody = List(RetrieveAllSavingsAccount(incomeSourceId, accountName))
    "a valid request is supplied" should {
      "return a successful response with a List of RetrieveSavingsAccount and the correct correlationId" in new Test {

        val expectedDesResponse = DesResponse(correlationId, expectedResponseBody)

        MockedHttpClient.get[RetrieveAllSavingsAccountsConnectorOutcome](
          s"$baseUrl" + s"/income-tax/income-sources/nino/$nino?incomeSourceType=interest-from-uk-banks"
        ).returns(Future.successful(Right(expectedDesResponse)))

        val result: RetrieveAllSavingsAccountsConnectorOutcome =
          await(connector.retrieveAll(RetrieveAllSavingsAccountRequest(Nino(nino))))

        result shouldBe Right(expectedDesResponse)
      }
    }

    "a request returning an error is supplied" should {
      "return an error response with a single error and the correct correlationId" in new Test {

        val expectedDesResponse = DesResponse(correlationId, SingleError(NinoFormatError))

        MockedHttpClient.get[RetrieveAllSavingsAccountsConnectorOutcome](
          s"$baseUrl" + s"/income-tax/income-sources/nino/$nino?incomeSourceType=interest-from-uk-banks"
        ).returns(Future.successful(Left(expectedDesResponse)))

        val result: RetrieveAllSavingsAccountsConnectorOutcome =
          await(connector.retrieveAll(RetrieveAllSavingsAccountRequest(Nino(nino))))

        result shouldBe Left(expectedDesResponse)
      }
      "return an error response with multiple errors and the correct correlationId" in new Test {

        val expectedDesResponse = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, NotFoundError)))

        MockedHttpClient.get[RetrieveAllSavingsAccountsConnectorOutcome](
          s"$baseUrl" + s"/income-tax/income-sources/nino/$nino?incomeSourceType=interest-from-uk-banks"
        ).returns(Future.successful(Left(expectedDesResponse)))

        val result: RetrieveAllSavingsAccountsConnectorOutcome =
          await(connector.retrieveAll(RetrieveAllSavingsAccountRequest(Nino(nino))))

        result shouldBe Left(expectedDesResponse)
      }
    }
  }

  "Retrieve single account" when {


    val expectedResponseBody = List(RetrieveSavingsAccount(incomeSourceId))
    "a valid request is supplied" should {
      "return a successful response with a List of RetrieveSavingsAccount and the correct correlationId" in new Test {
        val expectedDesResponse = DesResponse(correlationId, expectedResponseBody)

        MockedHttpClient.get[RetrieveSavingsAccountConnectorOutcome](
          s"$baseUrl" + s"/income-tax/income-sources/nino/$nino?incomeSourceType=interest-from-uk-banks&incomeSourceId=$incomeSourceId"
        ).returns(Future.successful(Right(expectedDesResponse)))

        val result: RetrieveSavingsAccountConnectorOutcome =
          await(connector.retrieve(RetrieveSavingsAccountRequest(Nino(nino), incomeSourceId)))

        result shouldBe Right(expectedDesResponse)
      }
    }

    "a request returning an error is supplied" should {
      "return an error response with a single error and the correct correlationId" in new Test {

        val expectedDesResponse = DesResponse(correlationId, SingleError(NinoFormatError))

        MockedHttpClient.get[RetrieveSavingsAccountConnectorOutcome](
          s"$baseUrl" + s"/income-tax/income-sources/nino/$nino?incomeSourceType=interest-from-uk-banks&incomeSourceId=$incomeSourceId"
        ).returns(Future.successful(Left(expectedDesResponse)))

        val result: RetrieveSavingsAccountConnectorOutcome =
          await(connector.retrieve(RetrieveSavingsAccountRequest(Nino(nino), incomeSourceId)))

        result shouldBe Left(expectedDesResponse)
      }


      "return an error response with multiple errors and the correct correlationId" in new Test {

        val expectedDesResponse = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, MatchingResourceNotFoundError)))

        MockedHttpClient.get[RetrieveSavingsAccountConnectorOutcome](
          s"$baseUrl" + s"/income-tax/income-sources/nino/$nino?incomeSourceType=interest-from-uk-banks&incomeSourceId=$incomeSourceId"
        ).returns(Future.successful(Left(expectedDesResponse)))

        val result: RetrieveSavingsAccountConnectorOutcome =
          await(connector.retrieve(RetrieveSavingsAccountRequest(Nino(nino), incomeSourceId)))

        result shouldBe Left(expectedDesResponse)
      }
    }
  }

}
