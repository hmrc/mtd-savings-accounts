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

package v2.models.audit

import play.api.http.Status
import play.api.libs.json.Json
import support.UnitSpec

class CreateSavingsAccountAuditDetailSpec extends UnitSpec {

  private val userType = "Organisation"
  private val agentReferenceNumber = Some("012345678")
  private val nino = "AA123456A"
  private val `X-CorrelationId` = "X-123"
  private val accountName = "myaccount"
  private val responseSuccess = CreateSavingsAccountAuditResponse(Status.CREATED, None, Some("0123IS12334567890"))
  private val responseFail = CreateSavingsAccountAuditResponse(Status.BAD_REQUEST, Some(Seq(AuditError("FORMAT_NINO"))), None)

  "writes" when {
    "passed an audit model with all fields provided" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""
             |{
             |  "userType": "Organisation",
             |  "agentReferenceNumber": "012345678",
             |  "nino": "AA123456A",
             |  "request": {
             |    "accountName": "$accountName"
             |  },
             |  "X-CorrelationId": "X-123",
             |  "response": {
             |    "httpStatus": 201,
             |    "savingsAccountId": "0123IS12334567890"
             |  }
             |}
           """.stripMargin)

        val request = Json.obj("accountName" -> accountName)

        val model = CreateSavingsAccountAuditDetail(userType, agentReferenceNumber, nino, request, `X-CorrelationId`, responseSuccess)

        Json.toJson(model) shouldBe json
      }
    }

    "passed an audit model with only mandatory fields provided" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""
             |{
             |  "userType": "Organisation",
             |  "nino": "AA123456A",
             |  "request": {
             |    "accountName": "$accountName"
             |  },
             |  "X-CorrelationId": "X-123",
             |  "response": {
             |    "httpStatus": 400,
             |    "errors": [
             |      {
             |        "errorCode": "FORMAT_NINO"
             |      }
             |    ]
             |  }
             |}
           """.stripMargin)

        val request = Json.obj("accountName" -> accountName)

        val model = CreateSavingsAccountAuditDetail(userType, None, nino, request, `X-CorrelationId`, responseFail)

        Json.toJson(model) shouldBe json
      }
    }
  }
}
