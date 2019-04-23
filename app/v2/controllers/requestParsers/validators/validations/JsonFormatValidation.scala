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

package v2.controllers.requestParsers.validators.validations

import play.api.Logger
import play.api.libs.json._
import play.api.mvc.AnyContentAsJson
import v2.models.errors.{BadRequestError, Error}

object JsonFormatValidation {

  val JSON_FIELD_MISSING = "JSON_FIELD_MISSING"
  val JSON_STRING_EXPECTED = "JSON_STRING_EXPECTED"
  val JSON_NUMBER_EXPECTED = "JSON_NUMBER_EXPECTED"
  val JSON_INTEGER_EXPECTED = "JSON_INTEGER_EXPECTED"
  val JSON_BOOLEAN_EXPECTED = "JSON_BOOLEAN_EXPECTED"
  val JSON_OBJECT_EXPECTED = "JSON_OBJECT_EXPECTED"
  val JSON_ARRAY_EXPECTED = "JSON_ARRAY_EXPECTED"

  private val logger: Logger = Logger(this.getClass)

  def validate[A](data: AnyContentAsJson)(implicit reads: Reads[A]): List[Error] = {

    data.json.validate[A] match {
      case JsSuccess(_, _) => NoValidationErrors
      case JsError(errors) => convertJsErrors(errors)
    }

  }

  private def convertJsErrors(errors: Seq[(JsPath, Seq[JsonValidationError])]): List[Error] = {
    errors.toList.flatMap { data =>
      val (path, jsonValidationErrors) = data
      jsonValidationErrors.flatMap(error => mapSingleJsError(error, path))
    }
  }

  private def mapSingleJsError(jsonError: JsonValidationError, path: JsPath): List[Error] = {
    jsonError.messages.map {
      case "error.path.missing" => Error(JSON_FIELD_MISSING, s"$path is missing")
      case "error.expected.jsstring" => Error(JSON_STRING_EXPECTED, s"$path should be a valid JSON string")
      case "error.expected.numberformatexception" | "error.expected.jsnumberorjsstring" => Error(JSON_NUMBER_EXPECTED, s"$path should be a valid JSON number")
      case "error.expected.int" => Error(JSON_INTEGER_EXPECTED, s"$path should be a valid integer")
      case "error.expected.jsboolean" => Error(JSON_BOOLEAN_EXPECTED, s"$path should be a valid JSON boolean")
      case "error.expected.jsobject" => Error(JSON_OBJECT_EXPECTED, s"$path should be a valid JSON object")
      case "error.expected.jsarray" => Error(JSON_ARRAY_EXPECTED, s"$path should be a valid JSON array")
      case unmatched => {
        logger.warn(s"[JsonFormatValidation][mapSingleJsError] - Received '$unmatched' error type and was unable to map")
        BadRequestError
      }
    }
  }.toList

}
