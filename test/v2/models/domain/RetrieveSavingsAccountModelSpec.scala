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

package v2.models.domain

import play.api.libs.json._
import support.UnitSpec
import v2.models.utils.JsonErrorValidators

class RetrieveSavingsAccountModelSpec extends UnitSpec with JsonErrorValidators {

  val id1 = "SAVKB2UVwUTBQGJ"
  val accountName1 = "Main account name"
  val id2 = "SAVKB2UVwUTBQGK"
  val accountName2 = "Shares savings account"

  val model = List(RetrieveSavingsAccount(id1, accountName1), RetrieveSavingsAccount(id2, accountName2))

  val multipleJsonFromDes =
    Json.parse(s"""
      |[
      |    {
      |        "incomeSourceId": "$id1",
      |        "incomeSourceName": "$accountName1"
      |    },
      |    {
      |        "incomeSourceId": "$id2",
      |        "incomeSourceName": "$accountName2"
      |    }
      |]
    """.stripMargin)

  val multipleJsonToVendor =
    Json.parse(s"""
      |{
      |    "savingsAccounts": [
      |        {
      |            "id": "$id1",
      |            "accountName": "$accountName1"
      |        },
      |        {
      |            "id": "$id2",
      |            "accountName": "$accountName2"
      |        }
      |    ]
      |}
    """.stripMargin)

  val retrieveSavingsAccountModelAsJson =
    Json.parse(s"""
      |{
      |    "id": "$id1",
      |    "accountName": "$accountName1"
      |}
    """.stripMargin)

  val singleDesJsonsingleDesJson =
    Json.parse(s"""
      |{
      |    "incomeSourceId": "$id1",
      |    "incomeSourceName": "$accountName1"
      |}
    """.stripMargin)

  "reads" when {
    "passed a valid JSON array from DES" should {
      "read successfully as a List[RetrieveSavingsAccountModel]" in {
        multipleJsonFromDes.as[List[RetrieveSavingsAccount]] shouldBe model
      }
    }

    testMandatoryProperty[RetrieveSavingsAccount](singleDesJsonsingleDesJson)("/incomeSourceId")

    testPropertyType[RetrieveSavingsAccount](singleDesJsonsingleDesJson)(
      path = "/incomeSourceId",
      replacement = 12344.toJson,
      expectedError = JsonError.STRING_FORMAT_EXCEPTION
    )

    testMandatoryProperty[RetrieveSavingsAccount](singleDesJsonsingleDesJson)("/incomeSourceName")

    testPropertyType[RetrieveSavingsAccount](singleDesJsonsingleDesJson)(
      path = "/incomeSourceName",
      replacement = 12344.toJson,
      expectedError = JsonError.STRING_FORMAT_EXCEPTION
    )
  }

  "writes" when {
    "passed a valid RetrieveSavingsAccountModel" should {
      "write it as correct JSON" in {
        retrieveSavingsAccountModelAsJson shouldBe RetrieveSavingsAccount.writes.writes(model.head)
      }
    }
  }

  "writesList" when {
    "passing valid JSON to vendors" should {
      "write it in the correct format, with the savingsAccounts field" in {
        multipleJsonToVendor shouldBe RetrieveSavingsAccount.writesList.writes(model)
      }
    }
  }

}
