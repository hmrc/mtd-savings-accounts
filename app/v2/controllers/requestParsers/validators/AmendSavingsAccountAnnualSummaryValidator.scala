/*
 * Copyright 2020 HM Revenue & Customs
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

import v2.controllers.requestParsers.validators.validations._
import v2.models.domain.SavingsAccountAnnualSummary
import v2.models.errors
import v2.models.errors._
import v2.models.requestData.AmendSavingsAccountAnnualSummaryRawData

class AmendSavingsAccountAnnualSummaryValidator extends Validator[AmendSavingsAccountAnnualSummaryRawData] {

  private def parameterFormatValidation: AmendSavingsAccountAnnualSummaryRawData => List[List[Error]] = { data =>
    val accountIdRegex = "^[A-Za-z0-9]{15}$"
    List(
      NinoValidation.validate(data.nino),
      RegexValidation.validate(AccountIdFormatError, data.savingsAccountId, accountIdRegex),
      TaxYearValidation.validate(data.taxYear)
    )
  }

  private def parameterRuleValidation: AmendSavingsAccountAnnualSummaryRawData => List[List[Error]] = { data =>
    List(
      MtdTaxYearValidation.validate(data.taxYear, RuleTaxYearNotSupportedError)
    )
  }

  private def bodyFormatValidator: AmendSavingsAccountAnnualSummaryRawData => List[List[Error]] = { data =>
    List(
      JsonFormatValidation.validate[SavingsAccountAnnualSummary](data.body, RuleIncorrectOrEmptyBodyError)
    )
  }


  private def bodyRuleValidator: AmendSavingsAccountAnnualSummaryRawData => List[List[Error]] = { data =>
    val body = data.body.json.as[SavingsAccountAnnualSummary]

    List(
      DefinedFieldValidation.validate(RuleIncorrectOrEmptyBodyError, body.taxedUkInterest, body.untaxedUkInterest)
    )
  }

  private def bodyFieldsValidator: AmendSavingsAccountAnnualSummaryRawData => List[List[Error]] = { data =>

    val body = data.body.json.as[SavingsAccountAnnualSummary]

    List(
      AmountValidation.validate(body.taxedUkInterest, TaxedInterestFormatError),
      AmountValidation.validate(body.untaxedUkInterest, UnTaxedInterestFormatError)
    )
  }

  private val validationSet = List(
    parameterFormatValidation,
    bodyFormatValidator,
    parameterRuleValidation,
    bodyRuleValidator,
    bodyFieldsValidator)

  override def validate(data: AmendSavingsAccountAnnualSummaryRawData): List[errors.Error] = {
    run(validationSet, data).distinct
  }
}
