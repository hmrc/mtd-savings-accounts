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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import v2.controllers.requestParsers._
import v2.models.audit.{AuditError, AuditEvent, AuditResponse, SavingsAccountsAuditDetail}
import v2.models.auth.UserDetails
import v2.models.domain.{RetrieveAllSavingsAccountResponse, RetrieveSavingsAccountResponse}
import v2.models.errors._
import v2.models.requestData._
import v2.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService, SavingsAccountsService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SavingsAccountsController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          createSavingsAccountRequestDataParser: CreateSavingsAccountRequestDataParser,
                                          retrieveAllSavingsAccountRequestDataParser: RetrieveAllSavingsAccountRequestDataParser,
                                          retrieveSavingsAccountRequestDataParser: RetrieveSavingsAccountRequestDataParser,
                                          savingsAccountService: SavingsAccountsService,
                                          auditService: AuditService,
                                          val cc: ControllerComponents
                                         ) extends AuthorisedController(cc) {

  val logger: Logger = Logger(this.getClass)

  def create(nino: String): Action[JsValue] = authorisedAction(nino).async(parse.json) { implicit request =>
    createSavingsAccountRequestDataParser.parseRequest(CreateSavingsAccountRawData(nino, AnyContentAsJson(request.body))) match {
      case Right(createSavingsAccountRequest) => savingsAccountService.create(createSavingsAccountRequest).map {
        case Right(desResponse) =>
          auditSubmission(createAuditDetails(nino, CREATED, request.request.body, desResponse.correlationId,
            request.userDetails, Some(desResponse.responseData.incomeSourceId)))
          logger.info(s"[SavingsAccountsController][create] - Success response received with CorrelationId: ${desResponse.correlationId}")
          Created(Json.toJson(desResponse.responseData)).withHeaders("X-CorrelationId" -> desResponse.correlationId,
            "Location" -> s"/self-assessment/ni/$nino/savings-accounts/${desResponse.responseData}")
        case Left(errorWrapper) =>
          val correlationId = getCorrelationId(errorWrapper)
          val result = processError(errorWrapper).withHeaders("X-CorrelationId" -> correlationId)
          auditSubmission(createAuditDetails(nino, result.header.status, request.request.body, correlationId,
            request.userDetails, None, Some(errorWrapper)))
          result
      }
      case Left(errorWrapper) =>
        val correlationId = getCorrelationId(errorWrapper)
        val result = processError(errorWrapper).withHeaders("X-CorrelationId" -> correlationId)
        auditSubmission(createAuditDetails(nino, result.header.status, request.request.body, correlationId,
          request.userDetails, None, Some(errorWrapper)))
        Future.successful(result)
    }
  }

  def retrieveAll(nino: String): Action[AnyContent] = authorisedAction(nino).async { implicit request =>
    retrieveAllSavingsAccountRequestDataParser.parseRequest(RetrieveAllSavingsAccountRawData(nino)) match {
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

  def retrieve(nino: String, accountId: String): Action[AnyContent] = authorisedAction(nino).async { implicit request =>
    retrieveSavingsAccountRequestDataParser.parseRequest(RetrieveSavingsAccountRawData(nino, accountId)) match {
      case Right(retrieveSavingsAccountRequest) => savingsAccountService.retrieve(retrieveSavingsAccountRequest).map {
        case Right(desResponse) =>
          logger.info(s"[SavingsAccountsController][retrieve] - Success response received with CorrelationId: ${desResponse.correlationId}")
          Ok(RetrieveSavingsAccountResponse.vendorWrites.writes(desResponse.responseData))
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
           | AccountIdFormatError
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

  private def createAuditDetails(nino: String,
                                 statusCode: Int,
                                 request: JsValue,
                                 correlationId: String,
                                 userDetails: UserDetails,
                                 savingsAccountId: Option[String],
                                 errorWrapper: Option[ErrorWrapper] = None
                                ): SavingsAccountsAuditDetail = {
    val auditResponse = errorWrapper.map {
      wrapper =>
        AuditResponse(statusCode, Some(wrapper.allErrors.map(error => AuditError(error.code))), None)
    }.getOrElse(AuditResponse(statusCode, None, savingsAccountId))

    SavingsAccountsAuditDetail(userDetails.userType, userDetails.agentReferenceNumber, nino, request, correlationId, auditResponse)
  }

  private def auditSubmission(details: SavingsAccountsAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("addASavingsAccount", "add-a-savings-account", details)
    auditService.auditEvent(event)
  }
}
