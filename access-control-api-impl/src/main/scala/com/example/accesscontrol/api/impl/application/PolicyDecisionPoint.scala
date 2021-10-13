package com.example.accesscontrol.api.impl.application

import com.example.accesscontrol.api.impl.domain.Decision

import scala.concurrent.Future

trait PolicyDecisionPoint {
  def makeDecision(
    targets: Array[PolicyDecisionPoint.Target],
    attributes: Array[PolicyDecisionPoint.Attribute]
  ): Future[Either[RuntimeException, Array[PolicyDecisionPoint.TargetedDecision]]]
}
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
