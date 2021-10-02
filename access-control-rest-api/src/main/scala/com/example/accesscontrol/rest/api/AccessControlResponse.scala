package com.example.accesscontrol.rest.api

import play.api.libs.json.{Format, Json}

/**
 * The access control decision class.
 */
case class AccessControlResponse(decisions: Array[ResultedDecision])

object AccessControlResponse {
  /**
   * Format for converting access control response object to and from JSON.
   *
   * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
   */
  implicit val format: Format[AccessControlResponse] = Json.format[AccessControlResponse]
}

case class ResultedDecision(objectType: String, action: String, decision: String)

object ResultedDecision {
  /**
   * Format for converting decision object to and from JSON.
   *
   * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
   */
  implicit val format: Format[ResultedDecision] = Json.format[ResultedDecision]
}
