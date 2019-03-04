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

import support.UnitSpec
import v2.models.errors.{AccountIdFormatError, NinoFormatError, RuleTaxYearNotSupportedError, TaxYearFormatError}
import v2.models.requestData.RetrieveSavingsAccountAnnualSummaryRawData

class RetrieveSavingsAccountAnnualSummaryValidatorSpec extends UnitSpec {


  val nino = "AA123456A"
  val taxYear = "2018-19"
  val savingsAccountId = "ASDFKLM123WQR13"
  val requestRawData = RetrieveSavingsAccountAnnualSummaryRawData(nino, taxYear, savingsAccountId)


  "validate" should {
    "return nino format error" when {
      "an invalid nino is supplied" in {

        val result = new RetrieveSavingsAccountAnnualSummaryValidator().validate(requestRawData.copy(nino = "AA123A"))
        result shouldBe List(NinoFormatError)

      }
    }

    "return taxYear format error" when {
      "an invalid taxYear is supplied" in {

        val result = new RetrieveSavingsAccountAnnualSummaryValidator().validate(requestRawData.copy(taxYear = "21111"))
        result shouldBe List(TaxYearFormatError)

      }
    }

    "return savings account id format error" when {
      "an invalid savingAccountId is supplied" in {

        val result = new RetrieveSavingsAccountAnnualSummaryValidator().validate(requestRawData.copy(savingsAccountId = "ASDF123KLM"))
        result shouldBe List(AccountIdFormatError)

      }
    }

    "return tax year rule error" when {
      "a valid formatted tax year but not supported by MTD is supplied" in {

        val result = new RetrieveSavingsAccountAnnualSummaryValidator().validate(requestRawData.copy(taxYear = "2016-17"))
        result shouldBe List(RuleTaxYearNotSupportedError)

      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {

        val result = new RetrieveSavingsAccountAnnualSummaryValidator().validate(requestRawData.copy(nino = "AA123A", taxYear = "20167"))
        result shouldBe List(NinoFormatError, TaxYearFormatError)

      }
    }

  }

}
