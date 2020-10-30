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

package v2.controllers.requestParsers

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.domain.Nino
import v2.controllers.requestParsers.validators.CreateSavingsAccountValidator
import v2.models.domain.CreateSavingsAccountRequest
import v2.models.errors.{BadRequestError, ErrorWrapper}
import v2.models.requestData.{CreateSavingsAccountRawData, CreateSavingsAccountRequestData}

class CreateSavingsAccountRequestDataParser @Inject()(validator: CreateSavingsAccountValidator) {

  val logger: Logger = Logger(this.getClass)

  def parseRequest(data: CreateSavingsAccountRawData)(implicit correlationId: String): Either[ErrorWrapper, CreateSavingsAccountRequestData] = {
    validator.validate(data) match {
      case List() =>
        logger.info(message = "[RequestParser][parseRequest] " +
          s"Validation successful for the request with correlationId : $correlationId")
        Right(CreateSavingsAccountRequestData(Nino(data.nino), data.body.json.as[CreateSavingsAccountRequest]))
      case err :: Nil =>
        logger.info(message = "[RequestParser][parseRequest] " +
          s"Validation failed with ${err.code} error for the request with correlationId : $correlationId")
        Left(ErrorWrapper(correlationId, err, None))
      case errs =>
        logger.info("[RequestParser][parseRequest] " +
          s"Validation failed with ${errs.map(_.code).mkString(",")} error for the request with correlationId : $correlationId")
        Left(ErrorWrapper(correlationId, BadRequestError, Some(errs)))
    }
  }
}
