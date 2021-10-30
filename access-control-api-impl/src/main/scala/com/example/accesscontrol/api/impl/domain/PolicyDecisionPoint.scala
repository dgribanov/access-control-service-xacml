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
  def makeDecision(
    targets: Array[Target],
    attributes: Array[Attribute]
  ): Future[Either[RuntimeException, Array[TargetedDecision]]]
}
