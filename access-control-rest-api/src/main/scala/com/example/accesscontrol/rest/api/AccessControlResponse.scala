package com.example.accesscontrol.rest.api

import play.api.libs.json.{Format, Json}

sealed abstract class AccessControlResponse
case class AccessControlSuccessResponse(decisions: Array[ResultedDecision]) extends AccessControlResponse
case class AccessControlError(errorMessage: String) extends AccessControlResponse

object AccessControlSuccessResponse {
  implicit val format: Format[AccessControlSuccessResponse] = Json.format[AccessControlSuccessResponse]
}

object AccessControlError {
  implicit val format: Format[AccessControlError] = Json.format[AccessControlError]
}

object AccessControlResponse {
  implicit val format: Format[AccessControlResponse] = Json.format[AccessControlResponse]
}

case class ResultedDecision(objectType: String, action: String, decision: String)

object ResultedDecision {
  implicit val format: Format[ResultedDecision] = Json.format[ResultedDecision]
}
