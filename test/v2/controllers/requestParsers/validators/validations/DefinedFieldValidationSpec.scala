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

import support.UnitSpec
import v2.models.errors.Error
import v2.models.utils.JsonErrorValidators

class DefinedFieldValidationSpec extends UnitSpec with JsonErrorValidators {

  val dummyError = Error("DUMMY_CODE", "dummy message")

  "validate" should {
    "return no errors" when {
      "top level optional fields exist" in {
        val validationResult = DefinedFieldValidation.validate(dummyError, Some(Unit), None)
        validationResult shouldBe empty
      }
    }

    "return an error" in {
      DefinedFieldValidation.validate(dummyError, None, None) shouldBe List(dummyError)
    }
  }
}
