/*
 * Copyright 2020 HM Revenue & Customs
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

package v2.controllers

import org.scalatest.OneInstancePerTest
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v2.fixtures.Fixtures._
import v2.mocks.requestParsers._
import v2.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService, MockSavingsAccountAnnualSummaryService}
import v2.models.audit._
import v2.models.des.DesAmendSavingsAccountAnnualSummaryResponse
import v2.models.domain._
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SavingsAccountAnnualSummaryControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAmendSavingsAccountAnnualSummaryRequestDataParser
    with MockRetrieveSavingsAccountAnnualSummaryRequestDataParser
    with MockSavingsAccountAnnualSummaryService
    with MockAuditService
    with OneInstancePerTest {

  trait Test {

    val hc = HeaderCarrier()

    val controller = new SavingsAccountAnnualSummaryController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      amendSavingsAccountAnnualSummaryRequestDataParser = mockAmendSavingsAccountAnnualSummaryRequestDataParser,
      retrieveSavingsAccountAnnualSummaryRequestDataParser = mockRetrieveSavingsAnnualSummaryRequestDataParser,
      savingsAccountAnnualSummaryService = mockSavingsAccountAnnualSummaryService,
      auditService = mockAuditService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  val nino = "AA123456A"
  val correlationId = "X-123"
  val id = "SAVKB2UVwUTBQGJ"
  val taxYear = "2017-18"
  val accountName = "Main account name"
  val location = s"/self-assessment/ni/$nino/savings-accounts/$id"

  "amend" when {

    val rawData = AmendSavingsAccountAnnualSummaryRawData(nino, taxYear, id, AnyContentAsJson(SavingsAccountsFixture.amendRequestJson()))

    val request = AmendSavingsAccountAnnualSummaryRequest(Nino(nino), DesTaxYear(taxYear), id,
      SavingsAccountAnnualSummary(Some(123.45), Some(456.78)))

    val response = DesAmendSavingsAccountAnnualSummaryResponse("FIXME")


    "passed a valid request" should {
      "return a successful response with header X-CorrelationId" in new Test {

        MockAmendSavingsAccountAnnualSummaryRequestDataParser.parse(rawData)
          .returns(Right(request))

        MockSavingsAccountAnnualSummaryService.amend(request)
          .returns(Future.successful(Right(DesResponse(correlationId, response))))

        val result: Future[Result] = controller.amend(nino, id, taxYear)(fakePutRequest(SavingsAccountsFixture.amendRequestJson()))
        status(result) shouldBe NO_CONTENT
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val detail = AmendAnnualSummaryAuditDetail(
          "Individual", None, nino, id, taxYear, SavingsAccountsFixture.amendRequestJson(), "X-123",
          AmendAnnualSummaryAuditResponse(NO_CONTENT, None))
        val event = AuditEvent("updateASavingsAccountAnnualSummary", "update-a-savings-account-annual-summary", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    "return single error response with status 400" when {
      "the request received failed the validation" in new Test() {

        MockAmendSavingsAccountAnnualSummaryRequestDataParser.parse(rawData)
          .returns(Left(ErrorWrapper(None, BadRequestError, None)))

        val result: Future[Result] = controller.amend(nino, id, taxYear)(fakePutRequest(SavingsAccountsFixture.amendRequestJson()))
        status(result) shouldBe BAD_REQUEST
        header("X-CorrelationId", result).nonEmpty shouldBe true

        val detail = AmendAnnualSummaryAuditDetail("Individual", None, nino, id, taxYear,
          SavingsAccountsFixture.amendRequestJson(), header("X-CorrelationId", result).get,
          AmendAnnualSummaryAuditResponse(BAD_REQUEST, Some(Seq(AuditError(BadRequestError.code)))))
        val event = AuditEvent("updateASavingsAccountAnnualSummary", "update-a-savings-account-annual-summary", detail)

        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    "return a 400 Bad Request with a single error" when {

      val badRequestErrorsFromParser = List(
        BadRequestError,
        NinoFormatError,
        TaxYearFormatError,
        RuleTaxYearNotSupportedError,
        RuleTaxYearRangeExceededError,
        TaxedInterestFormatError,
        UnTaxedInterestFormatError,
        AccountIdFormatError,
        RuleIncorrectOrEmptyBodyError
      )

      val badRequestErrorsFromService = List(
        NinoFormatError,
        BadRequestError,
        TaxYearFormatError,
        RuleTaxYearNotSupportedError
      )

      badRequestErrorsFromParser.foreach(errorsFromAmendParserTester(_, BAD_REQUEST))
      badRequestErrorsFromService.foreach(errorsFromAmendServiceTester(_, BAD_REQUEST))

    }

    "return a 500 Internal Server Error with a single error" when {

      val internalServerErrorErrors = List(
        DownstreamError
      )

      internalServerErrorErrors.foreach(errorsFromAmendParserTester(_, INTERNAL_SERVER_ERROR))
      internalServerErrorErrors.foreach(errorsFromAmendServiceTester(_, INTERNAL_SERVER_ERROR))

    }

    "return a 404 Not Found Error" when {
      val notFoundErrors = List(
        NotFoundError
      )

      notFoundErrors.foreach(errorsFromAmendServiceTester(_, NOT_FOUND))
    }

    def errorsFromAmendParserTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockAmendSavingsAccountAnnualSummaryRequestDataParser.parse(rawData)
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val response: Future[Result] = controller.amend(nino, id, taxYear)(fakePutRequest(SavingsAccountsFixture.amendRequestJson()))

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val detail = AmendAnnualSummaryAuditDetail("Individual", None, nino, id, taxYear, SavingsAccountsFixture.amendRequestJson(), "X-123",
          AmendAnnualSummaryAuditResponse(expectedStatus, Some(Seq(AuditError(error.code)))))
        val event = AuditEvent("updateASavingsAccountAnnualSummary",
          "update-a-savings-account-annual-summary", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    def errorsFromAmendServiceTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the service" in new Test {

        MockAmendSavingsAccountAnnualSummaryRequestDataParser.parse(rawData)
          .returns(Right(request))

        MockSavingsAccountAnnualSummaryService.amend(request)
          .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

        val response: Future[Result] = controller.amend(nino, id, taxYear)(fakePutRequest(SavingsAccountsFixture.amendRequestJson()))

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val detail = AmendAnnualSummaryAuditDetail("Individual", None, nino, id, taxYear, SavingsAccountsFixture.amendRequestJson(), "X-123",
          AmendAnnualSummaryAuditResponse(expectedStatus, Some(Seq(AuditError(error.code)))))
        val event = AuditEvent("updateASavingsAccountAnnualSummary",
          "update-a-savings-account-annual-summary", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }
  }

  "retrieve" when {

    val rawData = RetrieveSavingsAccountAnnualSummaryRawData(nino, taxYear, id)
    val request = RetrieveSavingsAccountAnnualSummaryRequest(Nino(nino), DesTaxYear(taxYear), id)
    val successRetrieveAnnualServiceResponse = SavingsAccountAnnualSummary(Some(5000.00), Some(5000.00))

    "passed a valid request" should {
      "return a successful response with header X-CorrelationId" in new Test {

        MockRetrieveSavingsAccountAnnualSummaryRequestDataParser.parse(rawData)
          .returns(Right(request))

        MockSavingsAccountAnnualSummaryService.retrieve(request)
          .returns(Future.successful(Right(DesResponse(correlationId, successRetrieveAnnualServiceResponse))))

        val result: Future[Result] = controller.retrieve(nino, id, taxYear)(fakeGetRequest)

        status(result) shouldBe OK
        contentAsJson(result) shouldBe SavingsAccountsFixture.retrieveAnnualJsonResponse("5000.00", "5000.00")
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }

    "the request received failed the validation" should {
      "return single error response with status 400" in new Test() {

        MockRetrieveSavingsAccountAnnualSummaryRequestDataParser.parse(rawData)
          .returns(Left(ErrorWrapper(None, BadRequestError, None)))

        val result: Future[Result] = controller.retrieve(nino, id, taxYear)(fakeGetRequest)

        status(result) shouldBe BAD_REQUEST
        header("X-CorrelationId", result).nonEmpty shouldBe true
      }
    }

    "return a 400 Bad Request with a single error" when {

      val badRequestErrorsFromParser = List(
        NinoFormatError,
        TaxYearFormatError,
        RuleTaxYearNotSupportedError,
        RuleTaxYearRangeExceededError,
        AccountIdFormatError
      )

      val badRequestErrorsFromService = List(
        BadRequestError,
        NinoFormatError,
        TaxYearFormatError,
        AccountIdFormatError
      )

      badRequestErrorsFromParser.foreach(errorsFromRetrieveParserTester(_, BAD_REQUEST))
      badRequestErrorsFromService.foreach(errorsFromRetrieveServiceTester(_, BAD_REQUEST))
    }

    "return a 404 Not Found Error" when {
      val notFoundErrors = List(
        NotFoundError
      )

      notFoundErrors.foreach(errorsFromRetrieveServiceTester(_, NOT_FOUND))
    }

    "return a 500 Internal Server Error with a single error" when {

      val internalServerErrorErrors = List(
        DownstreamError
      )

      internalServerErrorErrors.foreach(errorsFromRetrieveParserTester(_, INTERNAL_SERVER_ERROR))
      internalServerErrorErrors.foreach(errorsFromRetrieveServiceTester(_, INTERNAL_SERVER_ERROR))

    }

    def errorsFromRetrieveParserTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockRetrieveSavingsAccountAnnualSummaryRequestDataParser.parse(rawData)
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val response: Future[Result] = controller.retrieve(nino, id, taxYear)(fakeGetRequest)

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }

    def errorsFromRetrieveServiceTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the service" in new Test {

        MockRetrieveSavingsAccountAnnualSummaryRequestDataParser.parse(rawData)
          .returns(Right(request))

        MockSavingsAccountAnnualSummaryService.retrieve(request)
          .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

        val response: Future[Result] = controller.retrieve(nino, id, taxYear)(fakeGetRequest)

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }
  }
}
