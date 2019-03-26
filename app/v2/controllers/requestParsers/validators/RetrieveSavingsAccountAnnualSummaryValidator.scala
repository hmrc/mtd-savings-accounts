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

import v2.controllers.requestParsers.validators.validations.{MtdTaxYearValidation, NinoValidation, RegexValidation, TaxYearValidation}
import v2.models.errors
import v2.models.errors.{AccountIdFormatError, MtdError, RuleTaxYearNotSupportedError}
import v2.models.requestData.RetrieveSavingsAccountAnnualSummaryRawData

class RetrieveSavingsAccountAnnualSummaryValidator extends Validator[RetrieveSavingsAccountAnnualSummaryRawData] {

  private val validationSet = List(
    parameterFormatValidation,
    parameterRuleValidation)

  override def validate(data: RetrieveSavingsAccountAnnualSummaryRawData): List[errors.MtdError] = {
    run(validationSet, data).distinct
  }

  private def parameterFormatValidation: RetrieveSavingsAccountAnnualSummaryRawData => List[List[MtdError]] = { data =>
    val accountIdRegex = "^[A-Za-z0-9]{15}$"
    List(
      NinoValidation.validate(data.nino),
      RegexValidation.validate(AccountIdFormatError, data.savingsAccountId, accountIdRegex),
      TaxYearValidation.validate(data.taxYear)
    )
  }

  private def parameterRuleValidation: RetrieveSavingsAccountAnnualSummaryRawData => List[List[MtdError]] = { data =>
    List(
      MtdTaxYearValidation.validate(data.taxYear, RuleTaxYearNotSupportedError)
    )
  }
}
