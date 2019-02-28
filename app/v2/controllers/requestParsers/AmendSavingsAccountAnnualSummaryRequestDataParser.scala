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

import javax.inject.Inject
import uk.gov.hmrc.domain.Nino
import v2.controllers.requestParsers.validators.AmendSavingsAccountAnnualSummaryValidator
import v2.models.domain.SavingsAccountAnnualSummary
import v2.models.errors.{BadRequestError, ErrorWrapper}
import v2.models.requestData.{AmendSavingsAccountAnnualSummaryRawData, AmendSavingsAccountAnnualSummaryRequest, DesTaxYear}

import scala.util.Either

class AmendSavingsAccountAnnualSummaryRequestDataParser @Inject()(validator: AmendSavingsAccountAnnualSummaryValidator) {

  def parseRequest(data: AmendSavingsAccountAnnualSummaryRawData): Either[ErrorWrapper, AmendSavingsAccountAnnualSummaryRequest] = {
    validator.validate(data) match {
      case Nil =>
        //Validation passed.  Request data is ok to transform.
        Right(AmendSavingsAccountAnnualSummaryRequest(
          nino = Nino(data.nino),
          taxYear = DesTaxYear(data.taxYear),
          savingsAccountId = data.savingsAccountId,
          data.body.json.as[SavingsAccountAnnualSummary]))

      case err :: Nil => Left(ErrorWrapper(None, err, None))
      case errs       => Left(ErrorWrapper(None, BadRequestError, Some(errs)))
    }
  }
}
