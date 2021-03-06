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

package v2.controllers.requestParsers.validators

import v2.controllers.requestParsers.validators.validations.{NinoValidation, RegexValidation}
import v2.models.errors._
import v2.models.requestData.RetrieveSavingsAccountRawData

class RetrieveSavingsAccountValidator extends Validator[RetrieveSavingsAccountRawData] {

  private val validationSet = List(parameterFormatValidation)

  private def parameterFormatValidation: RetrieveSavingsAccountRawData => List[List[Error]] = (data: RetrieveSavingsAccountRawData) => {
    val accountIdRegex = "^[A-Za-z0-9]{15}$"
    List(
      NinoValidation.validate(data.nino),
      RegexValidation.validate(AccountIdFormatError, data.accountId, accountIdRegex)
    )
  }

  override def validate(data: RetrieveSavingsAccountRawData): List[Error] = {
    run(validationSet, data).distinct
  }

}
