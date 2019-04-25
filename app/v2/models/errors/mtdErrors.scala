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

package v2.models.errors

// Nino Errors
object NinoFormatError extends Error("FORMAT_NINO", "The provided NINO is invalid")

// Format Errors
object TaxYearFormatError extends Error("FORMAT_TAX_YEAR", "The provided tax year is invalid")
object AccountIdFormatError extends Error("FORMAT_SAVINGS_ACCOUNT_ID", "The provided savings account ID is invalid")
object AccountNameFormatError extends Error("FORMAT_ACCOUNT_NAME", "The provided account name is invalid")
object AccountNameMissingError extends Error("MISSING_ACCOUNT_NAME", "Account name field is required")
object TaxedInterestFormatError extends Error("FORMAT_TAXED_INTEREST", "The provided taxed interest amount is invalid")
object UnTaxedInterestFormatError extends Error("FORMAT_UNTAXED_INTEREST", "The provided untaxed UK interest amount is invalid")

// Rule Errors
object RuleTaxYearNotSupportedError extends Error("RULE_TAX_YEAR_NOT_SUPPORTED", "Tax year not supported, because it precedes the earliest allowable tax year")
object RuleTaxYearRangeExceededError extends Error("RULE_TAX_YEAR_RANGE_EXCEEDED", "Tax year range exceeded. A tax year range of one year is required.")
object AccountNameDuplicateError extends Error("RULE_DUPLICATE_ACCOUNT_NAME", "Duplicate account name given for supplied NINO")
object MaximumSavingsAccountsLimitError extends Error("RULE_MAXIMUM_SAVINGS_ACCOUNTS_LIMIT", "The 1000 savings account limit exceeded")
object RuleIncorrectOrEmptyBodyError extends Error("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED", "An empty or non-matching body was submitted")

//Standard Errors
object DownstreamError extends Error("INTERNAL_SERVER_ERROR", "An internal server error occurred")
object NotFoundError extends Error("MATCHING_RESOURCE_NOT_FOUND", "Matching resource not found")
object BadRequestError extends Error("INVALID_REQUEST", "Invalid request")
object BvrError extends Error("BUSINESS_ERROR", "Business validation error")
object ServiceUnavailableError extends Error("SERVICE_UNAVAILABLE", "Internal server error")

//Authorisation Errors
object UnauthorisedError extends Error("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised.")
