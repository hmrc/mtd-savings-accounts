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

package v2.controllers.requestParsers

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v2.controllers.requestParsers.validators.validations.JsonFormatValidation
import v2.mocks.validators.MockAmendSavingsAccountAnnualSummaryValidator
import v2.models.domain.SavingsAccountAnnualSummary
import v2.models.errors._
import v2.models.requestData.{AmendSavingsAccountAnnualSummaryRawData, AmendSavingsAccountAnnualSummaryRequest, DesTaxYear}

class AmendSavingsAccountAnnualSummaryRequestDataParserSpec
  extends UnitSpec {

  val nino = "AA123456A"
  val taxYear = "2018-19"
  val accountId = "SAVKB2UVwUTBQGJ"
  val json =
    """
      |{
      |    "taxedUkInterest": 123.45,
      |    "untaxedUkInterest": 543.21
      |}
    """.stripMargin
  val jsonBody = AnyContentAsJson(Json.parse(json))

  val model = SavingsAccountAnnualSummary(taxedUkInterest = Some(123.45), untaxedUkInterest = Some(543.21))


  val requestData =
    AmendSavingsAccountAnnualSummaryRawData(nino, taxYear, accountId, jsonBody)

  val request =
    AmendSavingsAccountAnnualSummaryRequest(Nino(nino), DesTaxYear.fromMtd(taxYear), accountId, model)

  trait Test extends MockAmendSavingsAccountAnnualSummaryValidator {
    lazy val parser = new AmendSavingsAccountAnnualSummaryRequestDataParser(mockValidator)
  }

  "parse" should {
    "return an create savings account request object" when {
      "valid request data is supplied" in new Test {
        MockAmendSavingsAccountAnnualSummaryValidator.validate(requestData)
          .returns(Nil)

        parser.parseRequest(requestData) shouldBe Right(request)
      }
    }

    "return an ErrorWrapper" when {

      "a single validation error occurs" in new Test {
        MockAmendSavingsAccountAnnualSummaryValidator.validate(requestData)
          .returns(List(NinoFormatError))

        parser.parseRequest(requestData) shouldBe Left(
          ErrorWrapper(None, NinoFormatError, None))
      }

      "multiple validation errors occur" in new Test {
        MockAmendSavingsAccountAnnualSummaryValidator.validate(requestData)
          .returns(List(NinoFormatError, AccountNameDuplicateError))


        parser.parseRequest(requestData) shouldBe Left(
          ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, AccountNameDuplicateError))))
      }


      "return a JSON validation error" when {
        "the supplied JSON fails validation" in new Test {

          private val expectedError = Error(JsonFormatValidation.JSON_NUMBER_EXPECTED, "/fieldName should be a valid JSON number")

          MockAmendSavingsAccountAnnualSummaryValidator.validate(requestData)
            .returns(List(expectedError))

          private val result = parser.parseRequest(requestData)

          result shouldBe Left(ErrorWrapper(None, BadRequestError, Some(List(expectedError))))
        }
      }
    }

  }
}
