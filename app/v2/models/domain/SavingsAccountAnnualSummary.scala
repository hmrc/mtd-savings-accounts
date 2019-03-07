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

package v2.models.domain

import play.api.libs.json.{Json, Reads, Writes}

case class SavingsAccountAnnualSummary(taxedUkInterest: Option[BigDecimal], untaxedUkInterest: Option[BigDecimal])

object SavingsAccountAnnualSummary {
  implicit val reads: Reads[SavingsAccountAnnualSummary] = Json.reads[SavingsAccountAnnualSummary]
  implicit val writes: Writes[SavingsAccountAnnualSummary] = Json.writes[SavingsAccountAnnualSummary]
}
