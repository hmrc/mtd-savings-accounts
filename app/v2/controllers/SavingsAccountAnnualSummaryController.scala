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

package v2.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, AnyContentAsJson, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import v2.controllers.requestParsers.{AmendSavingsAccountAnnualSummaryRequestDataParser, RetrieveSavingsAccountAnnualSummaryRequestDataParser}
import v2.models.audit._
import v2.models.auth.UserDetails
import v2.models.domain.SavingsAccountAnnualSummary
import v2.models.errors._
import v2.models.requestData._
import v2.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService, SavingsAccountAnnualSummaryService}
import v2.utils.IdGenerator

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SavingsAccountAnnualSummaryController @Inject()(
                                                    val authService: EnrolmentsAuthService,
                                                    val lookupService: MtdIdLookupService,
                                                    amendSavingsAccountAnnualSummaryRequestDataParser: AmendSavingsAccountAnnualSummaryRequestDataParser,
                                                    retrieveSavingsAccountAnnualSummaryRequestDataParser: RetrieveSavingsAccountAnnualSummaryRequestDataParser,
                                                    savingsAccountAnnualSummaryService: SavingsAccountAnnualSummaryService,
                                                    auditService: AuditService,
                                                    val idGenerator: IdGenerator,
                                                    val cc: ControllerComponents
                                                   ) (implicit ec: ExecutionContext) extends AuthorisedController(cc) {

  val logger: Logger = Logger(this.getClass)

  def amend(nino: String, accountId: String, taxYear: String): Action[JsValue] = authorisedAction(nino).async(parse.json) { implicit request =>
    implicit val endpointLogContext: EndpointLogContext =
      EndpointLogContext(controllerName = "SavingsAccountAnnualSummaryController", endpointName = "Amend savings account annual summary")
    implicit val correlationId: String = idGenerator.getCorrelationId
    logger.info(message = s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
      s"with correlationId : $correlationId")
    amendSavingsAccountAnnualSummaryRequestDataParser.parseRequest(
      AmendSavingsAccountAnnualSummaryRawData(
        nino, taxYear, accountId,
        AnyContentAsJson(request.body))) match {
      case Right(amendRequest) =>
        savingsAccountAnnualSummaryService.amend(amendRequest)
          .map {
            case Right(desResponse) =>
              logger.info(s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}]" +
                s" - Success response received with CorrelationId: ${desResponse.correlationId}")
              auditSubmission(createAuditDetails(
                nino = nino, savingsAccountId = accountId, taxYear = taxYear,
                statusCode = NO_CONTENT,
                request = request.request.body, correlationId = desResponse.correlationId,
                userDetails = request.userDetails))
              NoContent.withHeaders("X-CorrelationId" -> desResponse.correlationId)
            case Left(errorWrapper) =>
              val returnedCorrelationId = errorWrapper.correlationId
              val result = processError(errorWrapper).withHeaders("X-CorrelationId" -> returnedCorrelationId)
              logger.info(
                s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
                  s"Error response received with CorrelationId: $returnedCorrelationId")
              auditSubmission(details = createAuditDetails(
                nino = nino, savingsAccountId = accountId, taxYear = taxYear,
                statusCode = result.header.status,
                request = request.request.body, correlationId = returnedCorrelationId,
                userDetails = request.userDetails, errorWrapper = Some(errorWrapper)))

              result
          }
      case Left(errorWrapper) =>
        val returnedCorrelationId = errorWrapper.correlationId
        val result = processError(errorWrapper).withHeaders("X-CorrelationId" -> returnedCorrelationId)
        logger.info(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
            s"Error response received with CorrelationId: $returnedCorrelationId")
        auditSubmission(createAuditDetails(
          nino = nino, savingsAccountId = accountId, taxYear = taxYear,
          statusCode = result.header.status,
          request = request.request.body, correlationId = returnedCorrelationId,
          userDetails = request.userDetails, errorWrapper = Some(errorWrapper)))

        Future.successful(result)
    }
  }

  def retrieve(nino: String, accountId: String, taxYear: String): Action[AnyContent] = authorisedAction(nino).async { implicit request =>
    implicit val endpointLogContext: EndpointLogContext =
      EndpointLogContext(controllerName = "SavingsAccountAnnualSummaryController", endpointName = "Retrieve savings account annual summary")
    implicit val correlationId: String = idGenerator.getCorrelationId
    logger.info(message = s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
      s"with correlationId : $correlationId")

    retrieveSavingsAccountAnnualSummaryRequestDataParser.parseRequest(
      RetrieveSavingsAccountAnnualSummaryRawData(
        nino, taxYear, accountId)) match {
      case Right(retrieveRequest) => savingsAccountAnnualSummaryService.retrieve(retrieveRequest)
        .map {
          case Right(desResponse) =>
            logger.info(s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}]" +
              s" - Success response received with CorrelationId: ${desResponse.correlationId}")
            Ok(SavingsAccountAnnualSummary.writes.writes(desResponse.responseData))
              .withHeaders("X-CorrelationId" -> desResponse.correlationId)

          case Left(errorWrapper) => logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Error response received with CorrelationId: ${errorWrapper.correlationId}")
            processError(errorWrapper).withHeaders("X-CorrelationId" -> errorWrapper.correlationId)
        }
      case Left(errorWrapper) => logger.info(
        s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
          s"Error response received with CorrelationId: ${errorWrapper.correlationId}")
        Future.successful(processError(errorWrapper).withHeaders("X-CorrelationId" -> errorWrapper.correlationId))
    }
  }

  private def processError(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError
           | NinoFormatError
           | AccountIdFormatError
           | TaxYearFormatError
           | RuleTaxYearNotSupportedError
           | RuleTaxYearRangeExceededError
           | TaxedInterestFormatError
           | UnTaxedInterestFormatError
           | RuleIncorrectOrEmptyBodyError => BadRequest(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def createAuditDetails(nino: String,
                                 savingsAccountId: String,
                                 taxYear: String,
                                 statusCode: Int,
                                 request: JsValue,
                                 correlationId: String,
                                 userDetails: UserDetails,
                                 errorWrapper: Option[ErrorWrapper] = None
                                ) = {
    val response =
      errorWrapper
        .map { wrapper =>
          AmendAnnualSummaryAuditResponse(statusCode, Some(wrapper.allErrors.map(error => AuditError(error.code))))
        }
        .getOrElse(AmendAnnualSummaryAuditResponse(statusCode, None))

    AmendAnnualSummaryAuditDetail(
      userType = userDetails.userType,
      agentReferenceNumber = userDetails.agentReferenceNumber,
      nino = nino,
      savingsAccountId = savingsAccountId,
      taxYear = taxYear,
      request,
      correlationId,
      response)
  }

  private def auditSubmission(details: AmendAnnualSummaryAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext) = {
    val event = AuditEvent("updateASavingsAccountAnnualSummary", "update-a-savings-account-annual-summary", details)
    auditService.auditEvent(event)
  }
}
