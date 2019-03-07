/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIED OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v2.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v2.fixtures.Fixtures.SavingsAccountsFixture
import v2.models.errors._
import v2.models.requestData.DesTaxYear
import v2.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class SavingsAccountAnnualSummaryISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String = "AA123456A"
    val correlationId = "X-123"

    def setupStubs(): StubMapping

    def uri: String

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
    }
  }

  "Calling the amend savings account annual summary endpoint" should {

    trait AmendTest extends Test {
      val accountId = "SAVKB2UVwUTBQGJ"
      val taxYear = "2017-18"

      def uri = s"/2.0/ni/$nino/savings-accounts/$accountId/$taxYear"
    }

    val taxed = 123.45
    val untaxed = 543.21

    "return a 204 status code" when {

      "any valid request is made" in new AmendTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.amendSuccess(nino, accountId, DesTaxYear.fromMtd(taxYear))
        }

        val response: WSResponse = await(request().put(SavingsAccountsFixture.amendRequestJson(taxed, untaxed)))
        response.status shouldBe Status.NO_CONTENT
      }
    }

    "return 500 (Internal Server Error)" when {
      amendErrorTest(Status.BAD_REQUEST, "INVALID_TYPE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      amendErrorTest(Status.FORBIDDEN, "MISSING_CHARITIES_NAME_GIFT_AID", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      amendErrorTest(Status.FORBIDDEN, "MISSING_GIFT_AID_AMOUNT", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      amendErrorTest(Status.FORBIDDEN, "MISSING_CHARITIES_NAME_INVESTMENT", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      amendErrorTest(Status.FORBIDDEN, "MISSING_INVESTMENT_AMOUNT", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      amendErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      amendErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 400 (Bad Request)" when {
      amendErrorTest(Status.BAD_REQUEST, "INVALID_NINO", Status.BAD_REQUEST, NinoFormatError)
      amendErrorTest(Status.BAD_REQUEST, "INVALID_TAXYEAR", Status.BAD_REQUEST, TaxYearFormatError)
      amendErrorTest(Status.BAD_REQUEST, "INVALID_PAYLOAD", Status.BAD_REQUEST, BadRequestError)
      amendErrorTest(Status.FORBIDDEN, "INVALID_ACCOUNTING_PERIOD", Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
    }

    "return a 404 (Not Found)" when {
      amendErrorTest(Status.FORBIDDEN, "NOT_FOUND_INCOME_SOURCE", Status.NOT_FOUND, NotFoundError)
    }


    def amendErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"des returns an $desCode error" in new AmendTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.amendError(nino, accountId, DesTaxYear.fromMtd(taxYear), desStatus, errorBody(desCode))
        }

        val response: WSResponse = await(request().put(SavingsAccountsFixture.amendRequestJson(taxed, untaxed)))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
      }
    }

    "return 400 (Bad Request)" when {
      amendRequestValidationErrorTest("BADNINO", "SAVKB2UVwUTBQGJ", "2017-18", Status.BAD_REQUEST, NinoFormatError)
      amendRequestValidationErrorTest("AA123456A", "BADID", "2017-18", Status.BAD_REQUEST, AccountIdFormatError)
      amendRequestValidationErrorTest("AA123456A", "SAVKB2UVwUTBQGJ", "ABCD", Status.BAD_REQUEST, TaxYearFormatError)
    }

    def amendRequestValidationErrorTest(
                                         requestNino: String,
                                         requestAccountId: String,
                                         requestTaxYear: String,
                                         expectedStatus: Int, expectedBody: Error): Unit = {
      s"validation fails with ${expectedBody.code} error" in new AmendTest {

        override val nino: String = requestNino
        override val accountId: String = requestAccountId
        override val taxYear: String = requestTaxYear

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request().put(SavingsAccountsFixture.amendRequestJson(taxed, untaxed)))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
      }
    }

    s"incorrect body is supplied" in new AmendTest {
      val requestBody: JsValue = Json.parse(
        s"""{
           | "incorrectFieldName": "ABCDE"
           |}""".stripMargin
      )

      override def setupStubs(): StubMapping = {
        AuditStub.audit()
        AuthStub.authorised()
        MtdIdLookupStub.ninoFound(nino)
      }

      val response: WSResponse = await(request().put(requestBody))
      response.status shouldBe Status.BAD_REQUEST
      response.json shouldBe Json.toJson(ErrorWrapper(None, RuleIncorrectOrEmptyBodyError, None))
    }

    s"empty body is supplied" in new AmendTest {
      val requestBody: JsValue = Json.parse(
        s"""{
           |
           |}""".stripMargin
      )

      override def setupStubs(): StubMapping = {
        AuditStub.audit()
        AuthStub.authorised()
        MtdIdLookupStub.ninoFound(nino)
      }

      val response: WSResponse = await(request().put(requestBody))
      response.status shouldBe Status.BAD_REQUEST
      response.json shouldBe Json.toJson(ErrorWrapper(None, RuleIncorrectOrEmptyBodyError, None))
    }
  }


  def errorBody(code: String): String =
    s"""
       |      {
       |        "code": "$code",
       |        "reason": "des message"
       |      }
      """.stripMargin



























  "Calling the retrieve savings account annual summary endpoint" should {

    trait RetrieveTest extends Test {
      val accountId = "SAVKB2UVwUTBQGJ"
      val taxYear = "2017-18"

      def uri = s"/2.0/ni/$nino/savings-accounts/$accountId/$taxYear"
    }

    "return a 200 status code" when {

      "any valid request is made" in new RetrieveTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.retrieveAnnualSuccess(nino, accountId, DesTaxYear.fromMtd(taxYear))
        }

        val response: WSResponse = await(request().get())

        println("sfdfgdfgdnfgfuidsfhgoiosdjofgodfdgdfg/n")
        println(response)
        println("/nsfdfgdfgdnfgfuidsfhgoiosdjofgodfdgdfg")

        response.status shouldBe Status.OK

        response.json shouldBe Json.parse(
          s"""{
             |"taxedUKInterest": 5000.00,
             |"untaxedUKInterest": 5000.00
             |}
       """.stripMargin)
      }
    }

    "return 400 (Bad Request)" when {
      retrieveAnnualError(Status.BAD_REQUEST, "INVALID_NINO", Status.BAD_REQUEST, NinoFormatError)
      retrieveAnnualError(Status.BAD_REQUEST, "INVALID_TAXYEAR", Status.BAD_REQUEST, TaxYearFormatError)
      retrieveAnnualError(Status.BAD_REQUEST, "INVALID_INCOME_SOURCE", Status.BAD_REQUEST, AccountIdFormatError)

    }

    "return a 404 (Not Found)" when {
      retrieveAnnualError(Status.NOT_FOUND, "NOT_FOUND_PERIOD", Status.NOT_FOUND, NotFoundError)
      retrieveAnnualError(Status.NOT_FOUND, "NOT_FOUND_INCOME_SOURCE", Status.NOT_FOUND, NotFoundError)

    }

    "return 500 (Internal Server Error)" when {
      retrieveAnnualError(Status.BAD_REQUEST, "INVALID_TYPE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      retrieveAnnualError(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      retrieveAnnualError(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }


    def retrieveAnnualError(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"des returns an $desCode error" in new RetrieveTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.retrieveAnnualError(nino, accountId, DesTaxYear.fromMtd(taxYear), desStatus, errorBody(desCode))
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
      }
    }

    "return 400 (Bad Request)" when {
      retrieveRequestValidationErrorTest("BADNINO", "SAVKB2UVwUTBQGJ", "2017-18", Status.BAD_REQUEST, NinoFormatError)
      retrieveRequestValidationErrorTest("AA123456A", "BADID", "2017-18", Status.BAD_REQUEST, AccountIdFormatError)
      retrieveRequestValidationErrorTest("AA123456A", "SAVKB2UVwUTBQGJ", "ABCD", Status.BAD_REQUEST, TaxYearFormatError)
    }

    def retrieveRequestValidationErrorTest(
                                         requestNino: String,
                                         requestAccountId: String,
                                         requestTaxYear: String,
                                         expectedStatus: Int, expectedBody: Error): Unit = {
      s"validation fails with ${expectedBody.code} error" in new RetrieveTest {

        override val nino: String = requestNino
        override val accountId: String = requestAccountId
        override val taxYear: String = requestTaxYear

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
      }
    }
  }

}
