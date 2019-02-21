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

object DesStub extends WireMockMethods {

  private def savingsIncomeUrl(nino: String): String =
    s"/income-tax/income-sources/nino/$nino"

  private def getAllSavingsAccountsUrl(nino: String): String =
    s"/income-tax/income-sources/nino/$nino.*"

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

  private val retrieveResponseBody = Json.parse(
    """
      |[{
      | "incomeSourceId": "SAVKB2UVwUTBQGJ",
      | "incomeSourceName": "Main account name"
      |}]
    """.stripMargin)

  def retrieveSuccess(nino: String): StubMapping = {
    when(method = GET, uri = getAllSavingsAccountsUrl(nino))
      .thenReturn(status = OK, retrieveResponseBody)
  }

  def retrieveError(nino: String, errorStatus: Int, errorBody: String): StubMapping = {
    when(method = GET, uri = getAllSavingsAccountsUrl(nino))
      .thenReturn(status = errorStatus, errorBody)
  }

}
