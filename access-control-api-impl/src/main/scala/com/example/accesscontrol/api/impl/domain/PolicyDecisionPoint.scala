package com.example.accesscontrol.api.impl.domain

import scala.concurrent.Future

trait Target {
  val objectType: String
  val objectId: Int
  val action: String
}
trait Attribute {
  val name: String
  val value: AttributeValue
}
trait AttributeValue {
  val value: Any
}

trait PolicyDecisionPoint {
  case class PolicyCollectionFetchingError(errorMessage: String) extends RuntimeException
  case class PolicySetError(errorMessage: String) extends RuntimeException
  case class PolicyFetchingError(errorMessage: String) extends RuntimeException

  def makeDecision(
    subject: String,
    id: String,
    targets: Array[Target],
    attributes: Array[Attribute]
  ): Future[List[TargetedDecision]]
}
