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

class SavingsAccountsISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String
    val specificAccountId: Option[String] = None
    val specificTaxYear: Option[String] = None
    val correlationId = "X-123"

    def setupStubs(): StubMapping

    private def uri =
      (specificAccountId, specificTaxYear) match {
        case (None, None)         => s"/2.0/ni/$nino/savings-accounts"
        case (Some(id), Some(ty)) => s"/2.0/ni/$nino/savings-accounts/$id/$ty"
        case (Some(id), None)     => s"/2.0/ni/$nino/savings-accounts/$id"
        case _                    => throw new MatchError("invalid account/tax year combination")
      }

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
    }
  }

  "Calling the create savings account endpoint" should {

    "return a 201 status code" when {

      "any valid request is made" in new Test {
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.createSuccess(nino)
        }

        val response: WSResponse = await(request().post(SavingsAccountsFixture.createJson))
        response.status shouldBe Status.CREATED
      }
    }

    "return 500 (Internal Server Error)" when {

      createErrorTest(Status.BAD_REQUEST, "INVALID_IDTYPE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 400 (Bad Request)" when {
      createErrorTest(Status.BAD_REQUEST, "INVALID_IDVALUE", Status.BAD_REQUEST, NinoFormatError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_PAYLOAD", Status.BAD_REQUEST, BadRequestError)
    }

    "return 403 (Forbidden)" when {
      createErrorTest(Status.CONFLICT, "MAX_ACCOUNTS_REACHED", Status.FORBIDDEN, MaximumSavingsAccountsLimitError)
      createErrorTest(Status.CONFLICT, "ALREADY_EXISTS", Status.FORBIDDEN, AccountNameDuplicateError)
    }

    def createErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"des returns an $desCode error" in new Test {
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.createError(nino, desStatus, errorBody(desCode))
        }

        val response: WSResponse = await(request().post(SavingsAccountsFixture.createJson))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
      }
    }

    "return 400 (Bad Request)" when {
      createRequestValidationErrorTest("AA1123A", Status.BAD_REQUEST, NinoFormatError)
    }

    def createRequestValidationErrorTest(requestNino: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"validation fails with ${expectedBody.code} error" in new Test {

        override val nino: String = requestNino

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request().post(SavingsAccountsFixture.createJson))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
      }
    }

    s"incorrect body is supplied" in new Test {
      val requestBody: JsValue = Json.parse(
        s"""{
           | "accountName": "1*"
           |}""".stripMargin
      )

      override val nino: String = "AA123456A"

      override def setupStubs(): StubMapping = {
        AuditStub.audit()
        AuthStub.authorised()
        MtdIdLookupStub.ninoFound(nino)
      }

      val response: WSResponse = await(request().post(requestBody))
      response.status shouldBe Status.BAD_REQUEST
      response.json shouldBe Json.toJson(ErrorWrapper(None, AccountNameFormatError, None))
    }

    s"empty body is supplied" in new Test {
      val requestBody: JsValue = Json.parse(
        s"""{
           |
           |}""".stripMargin
      )

      override val nino: String = "AA123456A"

      override def setupStubs(): StubMapping = {
        AuditStub.audit()
        AuthStub.authorised()
        MtdIdLookupStub.ninoFound(nino)
      }

      val response: WSResponse = await(request().post(requestBody))
      response.status shouldBe Status.BAD_REQUEST
      response.json shouldBe Json.toJson(ErrorWrapper(None, AccountNameMissingError, None))
    }
  }

  "Calling the retrieve all savings account endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new Test {
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.retrieveAllSuccess(nino)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }

    "return 500 (Internal Server Error)" when {

      createErrorTest(Status.BAD_REQUEST, "INVALID_IDTYPE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_INCOMESOURCETYPE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_TAXYEAR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_INCOMESOURCEID", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_ENDDATE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 400 (Bad Request)" when {
      createErrorTest(Status.BAD_REQUEST, "INVALID_IDVALUE", Status.BAD_REQUEST, NinoFormatError)
    }

    "return 404 (Not Found)" when {
      createErrorTest(Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError)
    }

    def createErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"des returns an $desCode error" in new Test {
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.retrieveAllError(nino, desStatus, errorBody(desCode))
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
      }
    }

    "return 400 (Bad Request)" when {
      createRequestValidationErrorTest("AA1123A", Status.BAD_REQUEST, NinoFormatError)
    }

    def createRequestValidationErrorTest(requestNino: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"validation fails with ${expectedBody.code} error" in new Test {

        override val nino: String = requestNino

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


  "Calling the retrieve savings account endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new Test {

        val accountId = "SAVKB2UVwUTBQGJ"
        override val specificAccountId = Some(accountId)
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.retrieveSuccess(nino, accountId)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
        response.json shouldBe Json.parse(
          s"""
             |{
             |    "accountName": "Main account name"
             |}
          """.stripMargin)
      }
    }

    "return 500 (Internal Server Error)" when {

      createErrorTest(Status.BAD_REQUEST, "INVALID_IDTYPE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_INCOMESOURCETYPE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_TAXYEAR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_INCOMESOURCEID", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_ENDDATE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 400 (Bad Request)" when {
      createErrorTest(Status.BAD_REQUEST, "INVALID_IDVALUE", Status.BAD_REQUEST, NinoFormatError)
    }

    "return 404 (Not Found)" when {
      createErrorTest(Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError)
    }

    def createErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"des returns an $desCode error" in new Test {
        val accountId = "SAVKB2UVwUTBQGJ"
        override val specificAccountId = Some(accountId)
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.retrieveError(nino, accountId, desStatus, errorBody(desCode))
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
      }
    }

    "return 400 (Bad Request)" when {
      createRequestValidationErrorTest("BADNINO", "SAVKB2UVwUTBQGJ", Status.BAD_REQUEST, NinoFormatError)
      createRequestValidationErrorTest("AA123456A", "BADID", Status.BAD_REQUEST, AccountIdFormatError)
    }

    def createRequestValidationErrorTest(requestNino: String, accountId: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"validation fails with ${expectedBody.code} error" in new Test {
        override val specificAccountId = Some(accountId)
        override val nino: String = requestNino

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
      }
    }
  }


  "Calling the amend savings account annual summary endpoint" should {

    val taxed = 123.45
    val untaxed = 543.21

    "return a 200 status code" when {

      "any valid request is made" in new Test {
        val accountId = "SAVKB2UVwUTBQGJ"
        val taxYear = "2017-18"
        override val specificAccountId = Some(accountId)
        override val specificTaxYear = Some(taxYear)
        override val nino: String = "AA123456A"


        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.amendSuccess(nino, accountId, DesTaxYear.fromMtd(taxYear))
        }

        val response: WSResponse = await(request().put(SavingsAccountsFixture.amendRequestJson(taxed, untaxed)))
        response.status shouldBe Status.OK
      }
    }

    "return 500 (Internal Server Error)" when {
      amendErrorTest(Status.BAD_REQUEST, "INVALID_IDTYPE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      amendErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      amendErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 400 (Bad Request)" when {
      amendErrorTest(Status.BAD_REQUEST, "INVALID_TAXYEAR", Status.BAD_REQUEST, TaxYearFormatError)
      amendErrorTest(Status.BAD_REQUEST, "NOT_FOUND_INCOME_SOURCE", Status.NOT_FOUND, NotFoundError)
      amendErrorTest(Status.BAD_REQUEST, "INVALID_ACCOUNTING_PERIOD", Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
      amendErrorTest(Status.BAD_REQUEST, "INVALID_PAYLOAD", Status.BAD_REQUEST, BadRequestError)
    }


    def amendErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"des returns an $desCode error" in new Test {
        val accountId = "SAVKB2UVwUTBQGJ"
        val taxYear = "2017-18"
        override val specificAccountId = Some(accountId)
        override val specificTaxYear = Some(taxYear)
        override val nino: String = "AA123456A"

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
      amendRequestValidationErrorTest("AA1123A", Status.BAD_REQUEST, NinoFormatError)
    }

    def amendRequestValidationErrorTest(requestNino: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"validation fails with ${expectedBody.code} error" in new Test {

        val accountId = "SAVKB2UVwUTBQGJ"
        val taxYear = "2017-18"
        override val specificAccountId = Some(accountId)
        override val specificTaxYear = Some(taxYear)
        override val nino: String = requestNino

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

    s"incorrect body is supplied" in new Test {
      val requestBody: JsValue = Json.parse(
        s"""{
           | "accountName": "1*"
           |}""".stripMargin
      )

      override val nino: String = "AA123456A"

      override def setupStubs(): StubMapping = {
        AuditStub.audit()
        AuthStub.authorised()
        MtdIdLookupStub.ninoFound(nino)
      }

      val response: WSResponse = await(request().post(requestBody))
      response.status shouldBe Status.BAD_REQUEST
      response.json shouldBe Json.toJson(ErrorWrapper(None, AccountNameFormatError, None))
    }

    s"empty body is supplied" in new Test {
      val requestBody: JsValue = Json.parse(
        s"""{
           |
           |}""".stripMargin
      )

      override val nino: String = "AA123456A"

      override def setupStubs(): StubMapping = {
        AuditStub.audit()
        AuthStub.authorised()
        MtdIdLookupStub.ninoFound(nino)
      }

      val response: WSResponse = await(request().post(requestBody))
      response.status shouldBe Status.BAD_REQUEST
      response.json shouldBe Json.toJson(ErrorWrapper(None, AccountNameMissingError, None))
    }
  }


  def errorBody(code: String): String =
    s"""
       |      {
       |        "code": "$code",
       |        "reason": "des message"
       |      }
      """.stripMargin

}
