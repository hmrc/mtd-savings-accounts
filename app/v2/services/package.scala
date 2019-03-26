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

import v2.models.auth.UserDetails
import v2.models.des.DesAmendSavingsAccountAnnualSummaryResponse
import v2.models.domain._
import v2.models.errors.{Error, ErrorWrapper}
import v2.models.outcomes.DesResponse

package object services {

  type AuthOutcome = Either[Error, UserDetails]

  type CreateSavingsAccountOutcome = Either[ErrorWrapper, DesResponse[CreateSavingsAccountResponse]]
  type RetrieveAllSavingsAccountsOutcome = Either[ErrorWrapper, DesResponse[List[RetrieveAllSavingsAccountResponse]]]

  type RetrieveSavingsAccountsOutcome = Either[ErrorWrapper, DesResponse[RetrieveSavingsAccountResponse]]

  type AmendSavingsAccountAnnualSummaryOutcome = Either[ErrorWrapper, DesResponse[DesAmendSavingsAccountAnnualSummaryResponse]]

  type RetrieveSavingsAccountAnnualSummaryOutcome = Either[ErrorWrapper, DesResponse[SavingsAccountAnnualSummary]]
}
