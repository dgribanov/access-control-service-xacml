package com.example.accesscontrol.api.impl.application

import scala.concurrent.Future

object PolicyDecisionPoint {
  type Target = {
    val objectType: String
    val objectId: Int
    val action: String
  }
  type Attribute = {
    val name: String
    val value: AttributeValue
  }
  type AttributeValue = {
    val value: Any
  }
  type TargetedDecision = {
    val target: Target
    val decision: Future[Decision]
  }
}
trait PolicyDecisionPoint {
  import PolicyDecisionPoint._

  def makeDecision(
    targets: Array[Target],
    attributes: Array[Attribute]
  ): Future[Either[RuntimeException, Array[TargetedDecision]]]
}

trait Decision
