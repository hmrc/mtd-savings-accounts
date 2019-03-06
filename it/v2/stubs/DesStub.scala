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

package v2.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import support.WireMockMethods
import v2.models.requestData.DesTaxYear

object DesStub extends WireMockMethods {

  private def savingsIncomeUrl(nino: String): String =
    s"/income-tax/income-sources/nino/$nino"

  private def getAllSavingsAccountsUrl(nino: String): String =
    s"/income-tax/income-sources/nino/$nino"

  private def getAllSavingsAccountsQueryParams = Map("incomeSourceType" -> "interest-from-uk-banks")

  private def getSavingsAccountsUrl(nino: String): String =
    s"/income-tax/income-sources/nino/$nino"

  private def savingsAccountSummaryUrl(nino: String, accountId: String, taxYear: DesTaxYear): String =
    s"/income-tax/nino/$nino/income-source/savings/annual/${taxYear.value}"

  private def getSavingsAccountQueryParams(accountId: String) = Map("incomeSourceType" -> "interest-from-uk-banks", "incomeSourceId" -> accountId)

  private val createResponseBody = Json.parse(
    """
      |{
      | "incomeSourceId": "SAVKB2UVwUTBQGJ"
      |}
    """.stripMargin)

  def createSuccess(nino: String): StubMapping = {
    when(method = POST, uri = savingsIncomeUrl(nino))
      .thenReturn(status = OK, createResponseBody)
  }

  def createError(nino: String, errorStatus: Int, errorBody: String): StubMapping = {
    when(method = POST, uri = savingsIncomeUrl(nino))
      .thenReturn(status = errorStatus, errorBody)
  }

  private val retrieveAllResponseBody = Json.parse(
    """
      |[{
      | "incomeSourceId": "SAVKB2UVwUTBQGJ",
      | "incomeSourceName": "Main account name"
      |}]
    """.stripMargin)

  def retrieveAllSuccess(nino: String): StubMapping = {
    when(method = GET, uri = getAllSavingsAccountsUrl(nino), queryParams = getAllSavingsAccountsQueryParams)
      .thenReturn(status = OK, retrieveAllResponseBody)
  }

  def retrieveAllError(nino: String, errorStatus: Int, errorBody: String): StubMapping = {
    when(method = GET, uri = getAllSavingsAccountsUrl(nino), queryParams = getAllSavingsAccountsQueryParams)
      .thenReturn(status = errorStatus, errorBody)
  }


  private val retrieveResponseBody = Json.parse(
    """
      |[{
      | "incomeSourceName": "Main account name"
      |}]
    """.stripMargin)


  def retrieveSuccess(nino: String, accountId: String): StubMapping = {
    when(method = GET, uri = getSavingsAccountsUrl(nino), queryParams = getSavingsAccountQueryParams(accountId))
      .thenReturn(status = OK, retrieveResponseBody)
  }

  def retrieveError(nino: String, accountId: String, errorStatus: Int, errorBody: String): StubMapping = {
    when(method = GET, uri = getSavingsAccountsUrl(nino), queryParams = getSavingsAccountQueryParams(accountId))
      .thenReturn(status = errorStatus, errorBody)
  }

  private val amendSuccessResponseBody = Json.parse(
    s"""{
       |  "transactionReference": "0000000000000001"
       |}
    """.stripMargin)

  def amendSuccess(nino: String, accountId: String, taxYear: DesTaxYear): StubMapping = {
    when(method = POST, uri = savingsAccountSummaryUrl(nino, accountId, taxYear))
      .thenReturn(status = OK, amendSuccessResponseBody)
  }

  def retrieveAnnualSuccess(nino: String, accountId: String, taxYear: DesTaxYear): StubMapping = {
    when(method = GET, uri = savingsAccountSummaryUrl(nino, accountId, taxYear))
      .thenReturn(status = OK, retrieveAnnualSuccessResponseBody)
  }

  private val retrieveAnnualSuccessResponseBody = Json.parse(
    s"""{
       |"taxedUKInterest": 5000.00,
       |"untaxedUKInterest": 5000.00
       |}
      """.stripMargin
  )

  def amendError(nino: String, accountId: String, taxYear: DesTaxYear, errorStatus: Int, errorBody: String): StubMapping = {
    when(method = POST, uri = savingsAccountSummaryUrl(nino, accountId, taxYear))
      .thenReturn(status = errorStatus, errorBody)
  }

  def retrieveAnnualError(nino: String, accountId: String, taxYear: DesTaxYear, errorStatus: Int, errorBody: String): StubMapping = {
    when(method = GET, uri = getSavingsAccountsUrl(nino), queryParams = getSavingsAccountQueryParams(accountId))
      .thenReturn(status = errorStatus, errorBody)
  }
}
