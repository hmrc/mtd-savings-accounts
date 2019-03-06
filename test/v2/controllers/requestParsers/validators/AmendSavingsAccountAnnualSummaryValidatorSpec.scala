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

package v2.controllers.requestParsers.validators

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v2.models.errors._
import v2.models.requestData.AmendSavingsAccountAnnualSummaryRawData

class AmendSavingsAccountAnnualSummaryValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2018-19"
  private val validAccountId = "SAVKB2UVwUTBQGJ"
  private val validJson =
    """{
      |  "taxedUkInterest": 123.45,
      |  "untaxedUkInterest": 543.21
      |}
    """.stripMargin

  private def body(json: String) = AnyContentAsJson(Json.parse(json))

  private val validBody = body(validJson)

  private val validator = new AmendSavingsAccountAnnualSummaryValidator()

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(
          AmendSavingsAccountAnnualSummaryRawData(validNino, validTaxYear, validAccountId, validBody)
        ) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        validator.validate(
          AmendSavingsAccountAnnualSummaryRawData("AA123", validTaxYear, validAccountId, validBody)
        ) shouldBe List(NinoFormatError)
      }
    }

    "return a AccountIdFormatError error" when {
      "an invalid account id is supplied" in {
        validator.validate(
          AmendSavingsAccountAnnualSummaryRawData(validNino, validTaxYear, "BADID", validBody)
        ) shouldBe List(AccountIdFormatError)
      }
    }


    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in {
        validator.validate(
          AmendSavingsAccountAnnualSummaryRawData(validNino, "XXXX-YY", validAccountId, validBody)
        ) shouldBe List(TaxYearFormatError)
      }
    }


    "return RuleTaxYearNotSupportedError error" when {
      "an out of range tax year is supplied" in {
        validator.validate(
          AmendSavingsAccountAnnualSummaryRawData(validNino, "2016-17", validAccountId, validBody)
        ) shouldBe List(RuleTaxYearNotSupportedError)
      }
    }


    "return TaxedInterestFormatError error" when {
      "an invalid taxed interest figure is supplied" in {

        val json =
          """
            |{
            |  "taxedUkInterest": 123.4562342,
            |  "untaxedUkInterest": 543.21
            |}
          """.stripMargin

        validator.validate(
          AmendSavingsAccountAnnualSummaryRawData(validNino, validTaxYear, validAccountId, body(json))
        ) shouldBe List(TaxedInterestFormatError)
      }
    }


    "return UnTaxedInterestFormatError error" when {
      "an invalid untaxed interest figure is supplied" in {

        val json =
          """
            |{
            |  "taxedUkInterest": 123.45,
            |  "untaxedUkInterest": 543.213445
            |}
          """.stripMargin

        validator.validate(
          AmendSavingsAccountAnnualSummaryRawData(validNino, validTaxYear, validAccountId, body(json))
        ) shouldBe List(UnTaxedInterestFormatError)
      }
    }

    "return RuleIncorrectOrEmptyBodyError error" when {
      "an empty body is supplied" in {
        val json = "{}"

        validator.validate(
          AmendSavingsAccountAnnualSummaryRawData(validNino, validTaxYear, validAccountId, body(json))
        ) shouldBe List(RuleIncorrectOrEmptyBodyError)
      }
    }

    "return no errors" when {
      "a only taxed interest is supplied" in {
        val json = """{
                     |  "taxedUkInterest": 123.45
                     |}
                   """.stripMargin

        validator.validate(
          AmendSavingsAccountAnnualSummaryRawData(validNino, validTaxYear, validAccountId, body(json))
        ) shouldBe Nil
      }

      "a only untaxed interest is supplied" in {
        val json = """{
                     |  "untaxedUkInterest": 123.45
                     |}
                   """.stripMargin

        validator.validate(
          AmendSavingsAccountAnnualSummaryRawData(validNino, validTaxYear, validAccountId, body(json))
        ) shouldBe Nil
      }
    }
  }
}
