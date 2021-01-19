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

package v2.controllers.requestParsers.validators.validations

import v2.models.errors.Error

object AmountValidation {

  def validate(amount: Option[BigDecimal], error: Error): List[Error] = {

    if (amount.exists(x => x <= 99999999999.99 && x >= 0 && x.scale < 3) || amount.isEmpty) {
      NoValidationErrors
    } else {
      List(error)
    }

  }

}
