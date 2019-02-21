package v2.models.requestData

import play.api.mvc.AnyContentAsJson

case class RetrieveSavingsAccountRawData(nino: String, accountId: String, body: AnyContentAsJson)
