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

package v2.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.http.HeaderCarrier
import v2.fixtures.Fixtures._
import v2.mocks.MockIdGenerator
import v2.mocks.requestParsers._
import v2.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService, MockSavingsAccountsService}
import v2.models.audit.{AuditError, AuditEvent, CreateSavingsAccountAuditDetail, CreateSavingsAccountAuditResponse}
import v2.models.domain.{Nino, _}
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SavingsAccountsControllerSpec extends ControllerBaseSpec
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService
  with MockCreateSavingsAccountRequestDataParser
  with MockRetrieveAllSavingsAccountRequestDataParser
  with MockRetrieveSavingsAccountRequestDataParser
  with MockSavingsAccountsService
  with MockIdGenerator
  with MockAuditService {

  val nino = "AA123456A"
  val correlationId = "X-123"
  val id = "SAVKB2UVwUTBQGJ"
  val taxYear = "2017-18"
  val accountName = "Main account name"
  val location = s"/self-assessment/ni/$nino/savings-accounts/$id"

  trait Test {

    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new SavingsAccountsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      createSavingsAccountRequestDataParser = mockCreateSavingsAccountRequestDataParser,
      retrieveAllSavingsAccountRequestDataParser = mockRetrieveAllSavingsAccountRequestDataParser,
      retrieveSavingsAccountRequestDataParser = mockRetrieveSavingsAccountRequestDataParser,
      savingsAccountService = mockSavingsAccountService,
      auditService = mockAuditService,
      idGenerator = mockIdGenerator,
      cc = cc
    )

    MockIdGenerator.getCorrelationId.returns(correlationId)
    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  "create" when {
    val createSavingsAccountRequest: CreateSavingsAccountRequestData =
      CreateSavingsAccountRequestData(Nino(nino), SavingsAccountsFixture.createSavingsAccountRequestModel)

    "passed a valid request" should {
      "return a successful response with header X-CorrelationId" in new Test {
        MockCreateSavingsAccountRequestDataParser.parse(
          CreateSavingsAccountRawData(nino, AnyContentAsJson(SavingsAccountsFixture.createJson)))
          .returns(Right(createSavingsAccountRequest))

        MockSavingsAccountService.create(createSavingsAccountRequest)
          .returns(Future.successful(Right(DesResponse(correlationId, CreateSavingsAccountResponse(id)))))

        val result: Future[Result] = controller.create(nino)(fakePostRequest(SavingsAccountsFixture.createJson))
        status(result) shouldBe CREATED
        contentAsJson(result) shouldBe SavingsAccountsFixture.createJsonResponse(id)
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val detail: CreateSavingsAccountAuditDetail = CreateSavingsAccountAuditDetail("Individual", None, nino,
          SavingsAccountsFixture.createJson, "X-123", CreateSavingsAccountAuditResponse(CREATED, None, Some(id)))
        val event: AuditEvent[CreateSavingsAccountAuditDetail] = AuditEvent[CreateSavingsAccountAuditDetail]("addASavingsAccount",
          "add-a-savings-account", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    "return single error response with status 400" when {
      "the request received failed the validation" in new Test() {

        MockCreateSavingsAccountRequestDataParser.parse(
          CreateSavingsAccountRawData(nino, AnyContentAsJson(SavingsAccountsFixture.createJson)))
          .returns(Left(ErrorWrapper(correlationId, BadRequestError, None)))

        val result: Future[Result] = controller.create(nino)(fakePostRequest(SavingsAccountsFixture.createJson))
        status(result) shouldBe BAD_REQUEST
        header("X-CorrelationId", result).nonEmpty shouldBe true

        val detail: CreateSavingsAccountAuditDetail = CreateSavingsAccountAuditDetail("Individual", None, nino, SavingsAccountsFixture.createJson, "X-123",
          CreateSavingsAccountAuditResponse(BAD_REQUEST, Some(Seq(AuditError(BadRequestError.code))), None))
        val event: AuditEvent[CreateSavingsAccountAuditDetail] = AuditEvent[CreateSavingsAccountAuditDetail]("addASavingsAccount",
          "add-a-savings-account", detail)

        MockedAuditService.verifyAuditEvent(event.copy(detail = detail.copy(`X-CorrelationId` = header("X-CorrelationId", result).get))).once
      }
    }

    "return a 400 Bad Request with a single error" when {

      val badRequestErrorsFromParser = List(
        BadRequestError,
        NinoFormatError,
        AccountNameFormatError,
        AccountNameMissingError
      )

      val badRequestErrorsFromService = List(
        NinoFormatError,
        BadRequestError
      )

      badRequestErrorsFromParser.foreach(errorsFromCreateParserTester(_, BAD_REQUEST))
      badRequestErrorsFromService.foreach(errorsFromCreateServiceTester(_, BAD_REQUEST))

    }

    "return a 500 Internal Server Error with a single error" when {

      val internalServerErrorErrors = List(
        DownstreamError
      )

      internalServerErrorErrors.foreach(errorsFromCreateParserTester(_, INTERNAL_SERVER_ERROR))
      internalServerErrorErrors.foreach(errorsFromCreateServiceTester(_, INTERNAL_SERVER_ERROR))

    }

    "return a 404 Not Found Error" when {

      val notFoundErrors = List(
        NotFoundError
      )

      notFoundErrors.foreach(errorsFromCreateServiceTester(_, NOT_FOUND))

    }

    "return a valid error response" when {
      "multiple errors exist" in new Test() {
        MockCreateSavingsAccountRequestDataParser.parse(
          CreateSavingsAccountRawData(nino, AnyContentAsJson(SavingsAccountsFixture.createJson)))
          .returns(Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(Error("error 1", "message 1"), Error("error 2", "message 2"))))))

        val result: Future[Result] = controller.create(nino)(fakePostRequest(SavingsAccountsFixture.createJson))
        status(result) shouldBe BAD_REQUEST
        header("X-CorrelationId", result).nonEmpty shouldBe true

        val detail: CreateSavingsAccountAuditDetail = CreateSavingsAccountAuditDetail("Individual", None, nino, SavingsAccountsFixture.createJson, "X-123",
          CreateSavingsAccountAuditResponse(BAD_REQUEST, Some(Seq(AuditError("error 1"), AuditError("error 2"))), None))
        val event: AuditEvent[CreateSavingsAccountAuditDetail] = AuditEvent[CreateSavingsAccountAuditDetail]("addASavingsAccount",
          "add-a-savings-account", detail)

        MockedAuditService.verifyAuditEvent(event.copy(detail = detail.copy(`X-CorrelationId` = header("X-CorrelationId", result).get))).once
      }
    }

    def errorsFromCreateParserTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        val createSavingsAccountRawData: CreateSavingsAccountRawData =
          CreateSavingsAccountRawData(nino, AnyContentAsJson(SavingsAccountsFixture.createJson))

        MockCreateSavingsAccountRequestDataParser.parse(createSavingsAccountRawData)
          .returns(Left(ErrorWrapper(correlationId, error, None)))

        val response: Future[Result] = controller.create(nino)(fakePostRequest[JsValue](SavingsAccountsFixture.createJson))

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val detail: CreateSavingsAccountAuditDetail = CreateSavingsAccountAuditDetail("Individual", None, nino, SavingsAccountsFixture.createJson, "X-123",
          CreateSavingsAccountAuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
        val event: AuditEvent[CreateSavingsAccountAuditDetail] = AuditEvent[CreateSavingsAccountAuditDetail]("addASavingsAccount",
          "add-a-savings-account", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    def errorsFromCreateServiceTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the service" in new Test {

        val createSavingsAccountRawData: CreateSavingsAccountRawData =
          CreateSavingsAccountRawData(nino, AnyContentAsJson(SavingsAccountsFixture.createJson))

        MockCreateSavingsAccountRequestDataParser.parse(createSavingsAccountRawData)
          .returns(Right(createSavingsAccountRequest))

        MockSavingsAccountService.create(createSavingsAccountRequest)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, error, None))))

        val response: Future[Result] = controller.create(nino)(fakePostRequest[JsValue](SavingsAccountsFixture.createJson))

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val detail: CreateSavingsAccountAuditDetail = CreateSavingsAccountAuditDetail("Individual", None, nino, SavingsAccountsFixture.createJson, "X-123",
          CreateSavingsAccountAuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None))
        val event: AuditEvent[CreateSavingsAccountAuditDetail] = AuditEvent[CreateSavingsAccountAuditDetail]("addASavingsAccount",
          "add-a-savings-account", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }
  }

  "retrieveAll" when {
    val retrieveAllSavingsAccountRequest: RetrieveAllSavingsAccountRequest =
      RetrieveAllSavingsAccountRequest(Nino(nino))

    val successRetrieveAllServiceResponse = List(RetrieveAllSavingsAccountResponse(id, accountName))

    "passed a valid request" should {
      "return a successful response with header X-CorrelationId" in new Test {
        MockRetrieveAllSavingsAccountRequestDataParser.parse(RetrieveAllSavingsAccountRawData(nino))
          .returns(Right(retrieveAllSavingsAccountRequest))
        MockSavingsAccountService.retrieveAll(retrieveAllSavingsAccountRequest)
          .returns(Future.successful(Right(DesResponse(correlationId, successRetrieveAllServiceResponse))))

        val result: Future[Result] = controller.retrieveAll(nino)(fakeGetRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe SavingsAccountsFixture.retrieveAllJsonReponse(id, accountName)
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "the request received failed the validation" should {
      "return single error response with status 400" in new Test() {

        MockRetrieveAllSavingsAccountRequestDataParser.parse(
          RetrieveAllSavingsAccountRawData(nino))
          .returns(Left(ErrorWrapper(correlationId, BadRequestError, None)))

        val result: Future[Result] = controller.retrieveAll(nino)(fakeGetRequest)
        status(result) shouldBe BAD_REQUEST
        header("X-CorrelationId", result).nonEmpty shouldBe true
      }
    }

    "return a 400 Bad Request with a single error" when {

      val badRequestErrorsFromParser = List(
        BadRequestError,
        NinoFormatError
      )

      val badRequestErrorsFromService = List(
        NinoFormatError,
        BadRequestError
      )

      badRequestErrorsFromParser.foreach(errorsFromRetrieveAllParserTester(_, BAD_REQUEST))
      badRequestErrorsFromService.foreach(errorsFromRetrieveAllServiceTester(_, BAD_REQUEST))

    }

    "return a 500 Internal Server Error with a single error" when {

      val internalServerErrorErrors = List(
        DownstreamError
      )

      internalServerErrorErrors.foreach(errorsFromRetrieveAllParserTester(_, INTERNAL_SERVER_ERROR))
      internalServerErrorErrors.foreach(errorsFromRetrieveAllServiceTester(_, INTERNAL_SERVER_ERROR))

    }

    "return a 404 Not Found Error" when {

      val notFoundErrors = List(
        NotFoundError
      )

      notFoundErrors.foreach(errorsFromRetrieveAllServiceTester(_, NOT_FOUND))

    }

    def errorsFromRetrieveAllParserTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        val retrieveSavingsAccountRawData: RetrieveAllSavingsAccountRawData =
          RetrieveAllSavingsAccountRawData(nino)

        MockRetrieveAllSavingsAccountRequestDataParser.parse(retrieveSavingsAccountRawData)
          .returns(Left(ErrorWrapper(correlationId, error, None)))

        val response: Future[Result] = controller.retrieveAll(nino)(fakeGetRequest)

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }

    def errorsFromRetrieveAllServiceTester(error: Error, expectedStatus: Int): Unit = {
      s"a $error error is returned from the service" in new Test {

        val retrieveSavingsAccountRawData: RetrieveAllSavingsAccountRawData =
          RetrieveAllSavingsAccountRawData(nino)

        MockRetrieveAllSavingsAccountRequestDataParser.parse(retrieveSavingsAccountRawData)
          .returns(Right(retrieveAllSavingsAccountRequest))

        MockSavingsAccountService.retrieveAll(retrieveAllSavingsAccountRequest)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, error, None))))

        val response: Future[Result] = controller.retrieveAll(nino)(fakeGetRequest)

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }
  }

  "retrieve" when {
    val retrieveSavingsAccountRequest: RetrieveSavingsAccountRequest =
      RetrieveSavingsAccountRequest(Nino(nino), id)

    val successRetrieveServiceResponse = RetrieveSavingsAccountResponse(accountName)


    "passed a valid request" should {
      "return a successful response with header X-CorrelationId" in new Test {
        MockRetrieveSavingsAccountRequestDataParser.parse(RetrieveSavingsAccountRawData(nino, id))
          .returns(Right(retrieveSavingsAccountRequest))
        MockSavingsAccountService.retrieve(retrieveSavingsAccountRequest)
          .returns(Future.successful(Right(DesResponse(correlationId, successRetrieveServiceResponse))))

        val result: Future[Result] = controller.retrieve(nino, id)(fakeGetRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe SavingsAccountsFixture.retrieveJsonReponse(accountName)
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }


    "the request received failed the validation" should {
      "return single error response with status 400" in new Test {
        MockRetrieveSavingsAccountRequestDataParser.parse(RetrieveSavingsAccountRawData(nino, id))
          .returns(Left(ErrorWrapper(correlationId, BadRequestError, None)))

        val result: Future[Result] = controller.retrieve(nino, id)(fakeGetRequest)
        status(result) shouldBe BAD_REQUEST
        header("X-CorrelationId", result).nonEmpty shouldBe true
      }
    }

    "return a 400 Bad Request with a single error" when {
      val badRequestErrorsFromParser = List(
        AccountIdFormatError,
        BadRequestError,
        NinoFormatError
      )

      val badRequestErrorsFromService = List(
        AccountIdFormatError,
        NinoFormatError,
        BadRequestError
      )

      badRequestErrorsFromParser.foreach(errorsFromRetrieveParserTester(_, BAD_REQUEST))
      badRequestErrorsFromService.foreach(errorsFromRetrieveServiceTester(_, BAD_REQUEST))
    }

    "return a 500 Internal Server Error with a single error" when {
      val internalServerErrorErrors = List(
        DownstreamError
      )

      internalServerErrorErrors.foreach(errorsFromRetrieveParserTester(_, INTERNAL_SERVER_ERROR))
      internalServerErrorErrors.foreach(errorsFromRetrieveServiceTester(_, INTERNAL_SERVER_ERROR))
    }

    "return a 404 Not Found Error" when {
      val notFoundErrors = List(
        NotFoundError
      )

      notFoundErrors.foreach(errorsFromRetrieveServiceTester(_, NOT_FOUND))
    }


    def errorsFromRetrieveParserTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        val retrieveSavingsAccountRawData: RetrieveSavingsAccountRawData =
          RetrieveSavingsAccountRawData(nino, id)

        MockRetrieveSavingsAccountRequestDataParser.parse(retrieveSavingsAccountRawData)
          .returns(Left(ErrorWrapper(correlationId, error, None)))

        val response: Future[Result] = controller.retrieve(nino, id)(fakeGetRequest)

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }

    def errorsFromRetrieveServiceTester(error: Error, expectedStatus: Int): Unit = {
      s"a $error error is returned from the service" in new Test {

        val retrieveSavingsAccountRawData: RetrieveSavingsAccountRawData =
          RetrieveSavingsAccountRawData(nino, id)

        MockRetrieveSavingsAccountRequestDataParser.parse(retrieveSavingsAccountRawData)
          .returns(Right(retrieveSavingsAccountRequest))

        MockSavingsAccountService.retrieve(retrieveSavingsAccountRequest)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, error, None))))

        val response: Future[Result] = controller.retrieve(nino, id)(fakeGetRequest)

        status(response) shouldBe expectedStatus
        contentAsJson(response) shouldBe Json.toJson(error)
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }
  }
}
