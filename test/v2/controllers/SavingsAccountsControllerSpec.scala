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

package v2.controllers

import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.services.{MockEnrolmentsAuthService, MockMtdIdLookupService}
import v2.fixtures.Fixtures._
import v2.models.errors._

import scala.concurrent.Future

class SavingsAccountsControllerSpec extends ControllerBaseSpec {

  trait Test extends MockEnrolmentsAuthService
    with MockMtdIdLookupService {

    val hc = HeaderCarrier()

    val controller = new SavingsAccountsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  val nino = "AA123456A"
  val correlationId = "X-123"
  val id = "SAVKB2UVwUTBQGJ"
  val location = s"/self-assessment/ni/$nino/savings-accounts/$id"

  "create" when {
    "passed a valid request" should {
      "return a successful response with header X-CorrelationId" in new Test {
        val result = controller.create(nino)(fakePostRequest(SavingsAccountsFixture.createJson))
        status(result) shouldBe CREATED
        contentAsJson(result) shouldBe SavingsAccountsFixture.createJsonResponse(id)
        header("X-CorrelationId", result) shouldBe Some(correlationId)
        header("Location", result) shouldBe Some(correlationId)
      }
    }

    "passed an invalid request" should {
      List(
        NinoFormatError,
        AccountNameFormatError,
        AccountNameMissingError
      ).foreach (
        error =>
          s"return a single error when receiving a ${error.code} error from the parser" in new Test {
            val result: Future[Result] = controller.create(nino)(fakePostRequest(SavingsAccountsFixture.createJson))
            status(result) shouldBe BAD_REQUEST
            contentAsJson(result) shouldBe Json.toJson(error)
            header("X-CorrelationId", result) shouldBe Some(correlationId)
          }
      )

      "return an InternalServerError when receiving a DownstreamError from the parser" in new Test {
        val result: Future[Result] = controller.create(nino)(fakePostRequest(SavingsAccountsFixture.createJson))
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe SavingsAccountsFixture.downstreamErrorJson
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }

      "return an InternalServerError when receiving a DownstreamError from the service" in new Test {
        val result: Future[Result] = controller.create(nino)(fakePostRequest(SavingsAccountsFixture.createJson))
        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe SavingsAccountsFixture.downstreamErrorJson
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }

      "return multiple errors when receiving multiple errors from the parser" in new Test {
        val result: Future[Result] = controller.create(nino)(fakePostRequest(SavingsAccountsFixture.createJson))
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe SavingsAccountsFixture.multipleErrorsFromParserJson(correlationId)
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }

      "return multiple errors when receiving multiple errors from the service" in new Test {
        val result: Future[Result] = controller.create(nino)(fakePostRequest(SavingsAccountsFixture.createJson))
        status(result) shouldBe FORBIDDEN
        contentAsJson(result) shouldBe SavingsAccountsFixture.multipleErrorsFromServerJson(correlationId)
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }
  }

}
