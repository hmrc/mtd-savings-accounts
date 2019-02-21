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

package v2

import v2.models.domain.{RetrieveAllSavingsAccount, RetrieveSavingsAccount}
import v2.models.errors.{DesError, Error}
import v2.models.outcomes.DesResponse

package object connectors {

  type MtdIdLookupOutcome = Either[Error, String]

  type CreateSavingsAccountConnectorOutcome = Either[DesResponse[DesError], DesResponse[String]]

  type RetrieveAllSavingsAccountsConnectorOutcome = Either[DesResponse[DesError], DesResponse[List[RetrieveAllSavingsAccount]]]

  // Note: DES returns array of accounts - and so the connector returns these; and then we check exactly one account in the service layer
  type RetrieveSavingsAccountConnectorOutcome = Either[DesResponse[DesError], DesResponse[List[RetrieveSavingsAccount]]]


}
