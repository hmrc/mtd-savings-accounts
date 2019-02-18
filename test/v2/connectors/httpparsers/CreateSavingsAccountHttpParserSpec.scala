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

package v2.connectors.httpparsers

import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import uk.gov.hmrc.http.HttpResponse
import v2.models.errors._
import v2.models.outcomes.DesResponse


class CreateSavingsAccountHttpParserSpec extends UnitSpec {

  val method = "POST"
  val url = "test-url"

  val incomeSourceId = "ZZIS12345678901"
  val correlationId = "X-123"
  val desExpectedJson: JsValue = Json.obj("incomeSourceId" -> incomeSourceId)
  val desResponse = DesResponse(correlationId, incomeSourceId)

  "read" when {
    "the HTTP response status is 200" when {
      "return a Right DES response if the response is a String" in {

        val httpResponse = HttpResponse(OK, Some(desExpectedJson), Map("CorrelationId" -> Seq(correlationId)))

        val result = CreateSavingsAccountHttpParser.createHttpReads.read(method, url, httpResponse)
        result shouldBe Right(desResponse)
      }
      "return an outbound error if the response is not a String" in {

        val badFieldTypeJson: JsValue = Json.obj("incomeSourceId" -> 1234)
        val httpResponse = HttpResponse(OK, Some(badFieldTypeJson), Map("CorrelationId" -> Seq(correlationId)))
        val expected = DesResponse(correlationId, OutboundError(DownstreamError))

        val result = CreateSavingsAccountHttpParser.createHttpReads.read(method, url, httpResponse)
        result shouldBe Left(expected)
      }
    }

    List(BAD_REQUEST, FORBIDDEN, CONFLICT).foreach {
      response =>
        s"the HTTP response is $response" should {

          "be able to return a single error" in {
            val errorResponseJson = Json.parse(
              """
                |{
                |  "code": "TEST_CODE",
                |  "reason": "some reason"
                |}
              """.stripMargin)
            val expected = DesResponse(correlationId, SingleError(Error("TEST_CODE", "some reason")))

            val httpResponse = HttpResponse(response, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
            val result = CreateSavingsAccountHttpParser.createHttpReads.read(method, url, httpResponse)
            result shouldBe Left(expected)
          }

          "be able to return multiple errors" in {
            val errorResponseJson = Json.parse(
              """
                |{
                |	"failures" : [
                |    {
                |      "code": "TEST_CODE_1",
                |      "reason": "some reason"
                |    },
                |    {
                |      "code": "TEST_CODE_2",
                |      "reason": "some reason"
                |    }
                |  ]
                |}
              """.stripMargin)
            val expected = DesResponse(correlationId, MultipleErrors(Seq(Error("TEST_CODE_1", "some reason"), Error("TEST_CODE_2", "some reason"))))

            val httpResponse = HttpResponse(response, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
            val result = CreateSavingsAccountHttpParser.createHttpReads.read(method, url, httpResponse)
            result shouldBe Left(expected)
          }

          "be able to return an outbound error if the error JSON is not one which can be handled" in {
            val errorResponseJson = Json.parse(
              """
                |{
                |  "this": "TEST_CODE",
                |  "that": "some reason"
                |}
              """.stripMargin)
            val expected = DesResponse(correlationId, OutboundError(DownstreamError))

            val httpResponse = HttpResponse(response, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
            val result = CreateSavingsAccountHttpParser.createHttpReads.read(method, url, httpResponse)
            result shouldBe Left(expected)
          }
        }
    }

    List(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach {
      response =>
        s"the HTTP response is $response" should {
          "return an outbound error if the error JSON matches the Error model" in {
            val errorResponseJson = Json.parse(
              """
                |{
                |  "code": "TEST_CODE",
                |  "reason": "some reason"
                |}
              """.stripMargin)
            val expected = DesResponse(correlationId, OutboundError(DownstreamError))

            val httpResponse = HttpResponse(response, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
            val result = CreateSavingsAccountHttpParser.createHttpReads.read(method, url, httpResponse)
            result shouldBe Left(expected)
          }

          "return an outbound error if the error JSON doesn't match the Error model" in {
            val errorResponseJson = Json.parse(
              """
                |{
                |  "this": "TEST_CODE",
                |  "that": "some reason"
                |}
              """.stripMargin)
            val expected = DesResponse(correlationId, OutboundError(DownstreamError))

            val httpResponse = HttpResponse(response, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
            val result = CreateSavingsAccountHttpParser.createHttpReads.read(method, url, httpResponse)
            result shouldBe Left(expected)
          }
        }
    }

    "the HTTP response contains an unexpected status" should {

      val status = 499

      "return an outbound error if the error JSON matches the Error model" in {
        val errorResponseJson = Json.parse(
          """
            |{
            |  "code": "TEST_CODE",
            |  "reason": "some reason"
            |}
          """.stripMargin)
        val expected = DesResponse(correlationId, OutboundError(DownstreamError))

        val httpResponse = HttpResponse(status, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
        val result = CreateSavingsAccountHttpParser.createHttpReads.read(method, url, httpResponse)
        result shouldBe Left(expected)
      }

      "return an outbound error if the error JSON doesn't match the Error model" in {
        val errorResponseJson = Json.parse(
          """
            |{
            |  "this": "TEST_CODE",
            |  "that": "some reason"
            |}
          """.stripMargin)
        val expected = DesResponse(correlationId, OutboundError(DownstreamError))

        val httpResponse = HttpResponse(status, Some(errorResponseJson), Map("CorrelationId" -> Seq(correlationId)))
        val result = CreateSavingsAccountHttpParser.createHttpReads.read(method, url, httpResponse)
        result shouldBe Left(expected)
      }
    }

  }

}
