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
import play.api.mvc.{Action, AnyContent, AnyContentAsJson, ControllerComponents}
import v2.controllers.requestParsers.{CreateSavingsAccountRequestDataParser, RetrieveAllSavingsAccountRequestDataParser}
import v2.models.domain.RetrieveAllSavingsAccountResponse
import v2.models.errors._
import v2.models.requestData.{CreateSavingsAccountRawData, RetrieveAllSavingsAccountRawData}
import v2.services.{EnrolmentsAuthService, MtdIdLookupService, SavingsAccountsService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SavingsAccountsController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          createSavingsAccountRequestDataParser: CreateSavingsAccountRequestDataParser,
                                          retrieveSavingsAccountRequestDataParser: RetrieveAllSavingsAccountRequestDataParser,
                                          savingsAccountService: SavingsAccountsService,
                                          val cc: ControllerComponents
                                         ) extends AuthorisedController(cc) {

  val logger: Logger = Logger(this.getClass)

  def create(nino: String): Action[JsValue] = authorisedAction(nino).async(parse.json) { implicit request =>
    createSavingsAccountRequestDataParser.parseRequest(CreateSavingsAccountRawData(nino, AnyContentAsJson(request.body))) match {
      case Right(createSavingsAccountRequest) => savingsAccountService.create(createSavingsAccountRequest).map {
        case Right(desResponse) =>
          logger.info(s"[SavingsAccountsController][create] - Success response received with CorrelationId: ${desResponse.correlationId}")
          Created(Json.toJson(desResponse.responseData)).withHeaders("X-CorrelationId" -> desResponse.correlationId,
            "Location" -> s"/self-assessment/ni/$nino/savings-accounts/${desResponse.responseData}")
        case Left(errorWrapper) => processError(errorWrapper).withHeaders("X-CorrelationId" -> getCorrelationId(errorWrapper))
      }
      case Left(errorWrapper) => Future.successful {
        processError(errorWrapper).withHeaders("X-CorrelationId" -> getCorrelationId(errorWrapper))
      }
    }
  }

  def retrieveAll(nino: String): Action[AnyContent] = authorisedAction(nino).async { implicit request =>
    retrieveSavingsAccountRequestDataParser.parseRequest(RetrieveAllSavingsAccountRawData(nino)) match {
      case Right(retrieveSavingsAccountRequest) => savingsAccountService.retrieveAll(retrieveSavingsAccountRequest).map {
        case Right(desResponse) =>
          logger.info(s"[SavingsAccountsController][retrieveAll] - Success response received with CorrelationId: ${desResponse.correlationId}")
          Ok(RetrieveAllSavingsAccountResponse.writesList.writes(desResponse.responseData))
            .withHeaders("X-CorrelationId" -> desResponse.correlationId)
        case Left(errorWrapper) => processError(errorWrapper).withHeaders("X-CorrelationId" -> getCorrelationId(errorWrapper))
      }
      case Left(errorWrapper) => Future.successful(
        processError(errorWrapper).withHeaders("X-CorrelationId" -> getCorrelationId(errorWrapper))
      )
    }
  }

  private def processError(errorWrapper: ErrorWrapper) = {
    errorWrapper.error match {
      case BadRequestError
           | NinoFormatError
           | AccountNameFormatError
           | AccountNameMissingError => BadRequest(Json.toJson(errorWrapper))
      case AccountNameDuplicateError
           | MaximumSavingsAccountsLimitError => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def getCorrelationId(errorWrapper: ErrorWrapper): String = {
    errorWrapper.correlationId match {
      case Some(correlationId) => logger.info("[SavingsAccountsController][getCorrelationId] - " +
        s"Error received from DES ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
        correlationId
      case None =>
        val correlationId = UUID.randomUUID().toString
        logger.info("[SavingsAccountsController][getCorrelationId] - " +
          s"Validation error: ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
        correlationId
    }
  }
}
