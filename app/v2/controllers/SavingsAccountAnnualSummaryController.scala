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

package v2.controllers

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import v2.controllers.requestParsers.AmendSavingsAccountAnnualSummaryRequestDataParser
import v2.models.errors._
import v2.models.requestData._
import v2.services.{EnrolmentsAuthService, MtdIdLookupService, SavingsAccountAnnualSummaryService}

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SavingsAccountAnnualSummaryController @Inject()(val authService: EnrolmentsAuthService,
                                                      val lookupService: MtdIdLookupService,
                                                      amendSavingsAccountAnnualSummaryRequestDataParser: AmendSavingsAccountAnnualSummaryRequestDataParser,
                                                      savingsAccountAnnualSummaryService: SavingsAccountAnnualSummaryService,
                                                      val cc: ControllerComponents
                                                     ) extends AuthorisedController(cc) {

  val logger: Logger = Logger(this.getClass)


  def amend(nino: String, accountId: String, taxYear: String): Action[JsValue] = authorisedAction(nino).async(parse.json) { implicit request =>
    amendSavingsAccountAnnualSummaryRequestDataParser.parseRequest(
      AmendSavingsAccountAnnualSummaryRawData(
        nino, taxYear, accountId,
        AnyContentAsJson(request.body))) match {
      case Right(amendRequest) =>
        savingsAccountAnnualSummaryService.amend(amendRequest)
          .map {
            case Right(desResponse) =>
              logger.info(s"[SavingsAccountAnnualSummaryController][amend] - Success response received with CorrelationId: ${desResponse.correlationId}")
              Ok.withHeaders("X-CorrelationId" -> desResponse.correlationId)

            case Left(errorWrapper) =>
              processError(errorWrapper).withHeaders("X-CorrelationId" -> getCorrelationId(errorWrapper))
          }

      case Left(errorWrapper) =>
        Future.successful(
          processError(errorWrapper).withHeaders("X-CorrelationId" -> getCorrelationId(errorWrapper))
        )
    }
  }

  private def processError(errorWrapper: ErrorWrapper) = {
    errorWrapper.error match {
      case BadRequestError
           | NinoFormatError
           | AccountIdFormatError
           | TaxYearFormatError
           | RuleTaxYearNotSupportedError
           | TaxedInterestFormatError
           | UnTaxedInterestFormatError
           | RuleIncorrectOrEmptyBodyError    => BadRequest(Json.toJson(errorWrapper))
      case NotFoundError                      => NotFound(Json.toJson(errorWrapper))
      case DownstreamError                    => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def getCorrelationId(errorWrapper: ErrorWrapper): String = {
    errorWrapper.correlationId match {
      case Some(correlationId) => logger.info("[SavingsAccountAnnualSummaryController][getCorrelationId] - " +
        s"Error received from DES ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
        correlationId
      case None                =>
        val correlationId = UUID.randomUUID().toString
        logger.info("[SavingsAccountAnnualSummaryController][getCorrelationId] - " +
          s"Validation error: ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
        correlationId
    }
  }
}
