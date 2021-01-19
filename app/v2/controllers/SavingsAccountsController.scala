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

package v2.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, AnyContentAsJson, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import v2.controllers.requestParsers._
import v2.models.audit.{AuditError, AuditEvent, CreateSavingsAccountAuditDetail, CreateSavingsAccountAuditResponse}
import v2.models.auth.UserDetails
import v2.models.domain.{RetrieveAllSavingsAccountResponse, RetrieveSavingsAccountResponse}
import v2.models.errors._
import v2.models.requestData._
import v2.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService, SavingsAccountsService}
import v2.utils.IdGenerator

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SavingsAccountsController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          createSavingsAccountRequestDataParser: CreateSavingsAccountRequestDataParser,
                                          retrieveAllSavingsAccountRequestDataParser: RetrieveAllSavingsAccountRequestDataParser,
                                          retrieveSavingsAccountRequestDataParser: RetrieveSavingsAccountRequestDataParser,
                                          savingsAccountService: SavingsAccountsService,
                                          auditService: AuditService,
                                          idGenerator: IdGenerator,
                                          val cc: ControllerComponents
                                         )(implicit ec: ExecutionContext) extends AuthorisedController(cc) {

  val logger: Logger = Logger(this.getClass)

  def create(nino: String): Action[JsValue] = authorisedAction(nino).async(parse.json) { implicit request =>

    implicit val endpointLogContext: EndpointLogContext =
      EndpointLogContext(controllerName = "SavingsAccountsController", endpointName = "Create savings account")

    implicit val correlationId: String = idGenerator.getCorrelationId
    logger.info(message = s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
      s"with correlationId : $correlationId")

    createSavingsAccountRequestDataParser.parseRequest(CreateSavingsAccountRawData(nino, AnyContentAsJson(request.body))) match {
      case Right(createSavingsAccountRequest) => savingsAccountService.create(createSavingsAccountRequest).map {
        case Right(desResponse) =>
          auditSubmission(createAuditDetails(nino, CREATED, request.request.body, desResponse.correlationId,
            request.userDetails, Some(desResponse.responseData.incomeSourceId)))
          logger.info(s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}]" +
            s" - Success response received with CorrelationId: ${desResponse.correlationId}")
          Created(Json.toJson(desResponse.responseData)).withHeaders("X-CorrelationId" -> desResponse.correlationId,
            "Location" -> s"/self-assessment/ni/$nino/savings-accounts/${desResponse.responseData.incomeSourceId}")
        case Left(errorWrapper) =>
          val returnedCorrelationId = errorWrapper.correlationId
          val result = processError(errorWrapper).withHeaders("X-CorrelationId" -> returnedCorrelationId)
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Error response received with CorrelationId: $returnedCorrelationId")
          auditSubmission(createAuditDetails(nino, result.header.status, request.request.body, returnedCorrelationId,
            request.userDetails, None, Some(errorWrapper)))
          result
      }
      case Left(errorWrapper) =>
        val returnedCorrelationId = errorWrapper.correlationId
        val result = processError(errorWrapper).withHeaders("X-CorrelationId" -> returnedCorrelationId)
        logger.info(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
            s"Error response received with CorrelationId: $returnedCorrelationId")
        auditSubmission(createAuditDetails(nino, result.header.status, request.request.body, returnedCorrelationId,
          request.userDetails, None, Some(errorWrapper)))
        Future.successful(result)
    }
  }

  def retrieveAll(nino: String): Action[AnyContent] = authorisedAction(nino).async { implicit request =>

    implicit val endpointLogContext: EndpointLogContext =
      EndpointLogContext(controllerName = "SavingsAccountsController", endpointName = "Retrieve all savings accounts")

    implicit val correlationId: String = idGenerator.getCorrelationId
    logger.info(message = s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
      s"with correlationId : $correlationId")
    retrieveAllSavingsAccountRequestDataParser.parseRequest(RetrieveAllSavingsAccountRawData(nino)) match {
      case Right(retrieveSavingsAccountRequest) => savingsAccountService.retrieveAll(retrieveSavingsAccountRequest).map {
        case Right(desResponse) =>
          logger.info(s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}]" +
            s" - Success response received with CorrelationId: ${desResponse.correlationId}")
          Ok(RetrieveAllSavingsAccountResponse.writesList.writes(desResponse.responseData))
            .withHeaders("X-CorrelationId" -> desResponse.correlationId)
        case Left(errorWrapper) =>
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Error response received with CorrelationId: ${errorWrapper.correlationId}")

          processError(errorWrapper).withHeaders("X-CorrelationId" -> errorWrapper.correlationId)
      }
      case Left(errorWrapper) => logger.info(
        s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
          s"Error response received with CorrelationId: ${errorWrapper.correlationId}")

        Future.successful(processError(errorWrapper).withHeaders("X-CorrelationId" -> errorWrapper.correlationId)
      )
    }
  }

  def retrieve(nino: String, accountId: String): Action[AnyContent] = authorisedAction(nino).async { implicit request =>
    implicit val endpointLogContext: EndpointLogContext =
      EndpointLogContext(controllerName = "SavingsAccountsController", endpointName = "Retrieve a savings account")

    implicit val correlationId: String = idGenerator.getCorrelationId
    logger.info(message = s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
      s"with correlationId : $correlationId")

    retrieveSavingsAccountRequestDataParser.parseRequest(RetrieveSavingsAccountRawData(nino, accountId)) match {
      case Right(retrieveSavingsAccountRequest) => savingsAccountService.retrieve(retrieveSavingsAccountRequest).map {
        case Right(desResponse) =>
          logger.info(s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}]" +
            s" - Success response received with CorrelationId: ${desResponse.correlationId}")
          Ok(RetrieveSavingsAccountResponse.vendorWrites.writes(desResponse.responseData))
            .withHeaders("X-CorrelationId" -> desResponse.correlationId)
        case Left(errorWrapper) => logger.info(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
            s"Error response received with CorrelationId: ${errorWrapper.correlationId}")
          processError(errorWrapper).withHeaders("X-CorrelationId" -> errorWrapper.correlationId)
      }
      case Left(errorWrapper) => logger.info(
        s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
          s"Error response received with CorrelationId: ${errorWrapper.correlationId}")

        Future.successful(processError(errorWrapper).withHeaders("X-CorrelationId" -> errorWrapper.correlationId)
      )
    }
  }

  private def processError(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
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

  private def createAuditDetails(nino: String,
                                 statusCode: Int,
                                 request: JsValue,
                                 correlationId: String,
                                 userDetails: UserDetails,
                                 savingsAccountId: Option[String],
                                 errorWrapper: Option[ErrorWrapper] = None
                                ): CreateSavingsAccountAuditDetail = {
    val response = errorWrapper.map {
      wrapper =>
        CreateSavingsAccountAuditResponse(statusCode, Some(wrapper.allErrors.map(error => AuditError(error.code))), None)
    }.getOrElse(CreateSavingsAccountAuditResponse(statusCode, None, savingsAccountId))

    CreateSavingsAccountAuditDetail(userDetails.userType, userDetails.agentReferenceNumber, nino, request, correlationId, response)
  }

  private def auditSubmission(details: CreateSavingsAccountAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("addASavingsAccount", "add-a-savings-account", details)
    auditService.auditEvent(event)
  }
}
