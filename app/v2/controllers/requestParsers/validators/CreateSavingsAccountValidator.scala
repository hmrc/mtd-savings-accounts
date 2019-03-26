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

import v2.controllers.requestParsers.validators.validations._
import v2.models.domain.CreateSavingsAccountRequest
import v2.models.errors._
import v2.models.requestData.CreateSavingsAccountRawData

class CreateSavingsAccountValidator extends Validator[CreateSavingsAccountRawData] {

  private val validationSet = List(parameterFormatValidation, requestRuleValidation, bodyFieldsFormatValidation)

  private def parameterFormatValidation: CreateSavingsAccountRawData => List[List[MtdError]] = (data: CreateSavingsAccountRawData) => {
    List(
      NinoValidation.validate(data.nino)
    )
  }

  private def requestRuleValidation: CreateSavingsAccountRawData => List[List[MtdError]] = (data: CreateSavingsAccountRawData) => {
    List(
      JsonFormatValidation.validate[CreateSavingsAccountRequest](data.body, AccountNameMissingError)
    )
  }

  private def bodyFieldsFormatValidation: CreateSavingsAccountRawData => List[List[MtdError]] = (data: CreateSavingsAccountRawData) => {

    val createSavingsAccount = data.body.json.as[CreateSavingsAccountRequest]
    val accountNameRegex = "^[^\\*\\<\\>\\[\\]\\{\\}\\/\\:\\;\\?\\#\\`\\\"\\'\\%\\^\\~\\|\\$\\¬\\\\]{1,32}$"
    List(
      RegexValidation.validate(AccountNameFormatError, createSavingsAccount.accountName, accountNameRegex)
    )
  }

  override def validate(data: CreateSavingsAccountRawData): List[MtdError] = {
    run(validationSet, data).distinct
  }

}
