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

package v2.models.domain

import play.api.libs.json.{JsArray, JsValue, Json}
import support.UnitSpec
import v2.models.utils.JsonErrorValidators

class RetrieveSavingsAccountResponseModelSpec extends UnitSpec with JsonErrorValidators {

  val model = RetrieveSavingsAccountResponse("Bank Account 1")

  val multipleJsonFromDes: JsValue = Json.parse(
    """[
      |   {
      |      "incomeSourceId": "000000000000001",
      |      "incomeSourceName": "Bank Account 1",
      |      "identifier": "AA111111A",
      |      "incomeSourceType": "interest-from-uk-banks"
      |   },
      |   {
      |      "incomeSourceId": "000000000000002",
      |      "incomeSourceName": "Bank Account 2",
      |      "identifier": "AA111111A",
      |      "incomeSourceType": "interest-from-uk-banks"
      |   },
      |   {
      |      "incomeSourceId": "000000000000003",
      |      "incomeSourceName": "Bank Account 3",
      |      "identifier": "AA111111A",
      |      "incomeSourceType": "interest-from-uk-banks"
      |   }
      |]
      |""".stripMargin)

  val singleJsonFromDes: JsValue = Json.parse(
    """   {
      |      "incomeSourceId": "000000000000001",
      |      "incomeSourceName": "Bank Account 1",
      |      "identifier": "AA111111A",
      |      "incomeSourceType": "interest-from-uk-banks"
      |   }
      |""".stripMargin)

  val singleJsonFromDesArray = JsArray(Seq(singleJsonFromDes))

  val jsonToVendor: JsValue = Json.parse(
    """{
      |  "accountName": "Bank Account 1"
      |}""".stripMargin)

  "reads" when {
    "single account returned from des" should {
      "read to model" in {
        singleJsonFromDesArray.as[List[RetrieveSavingsAccountResponse]] shouldBe List(model)
      }
    }

    "multiple accounts returned from des" should {
      "read to model" in {
        singleJsonFromDesArray.as[List[RetrieveSavingsAccountResponse]] shouldBe List(model)
      }
    }

    testMandatoryProperty[RetrieveSavingsAccountResponse](singleJsonFromDes)("/incomeSourceName")

    testPropertyType[RetrieveSavingsAccountResponse](singleJsonFromDes)(
      path = "/incomeSourceName",
      replacement = 12344.toJson,
      expectedError = JsonError.STRING_FORMAT_EXCEPTION
    )
  }

  "writes" must {
    "write to vendor format" in {
      model.toJson shouldBe jsonToVendor
    }
  }

}
