package com.example.accesscontrol.api.impl.application

import com.example.accesscontrol.api.impl.domain.PolicyDecisionPointImpl.{PolicyCollectionFetch}
import com.example.accesscontrol.api.impl.domain.{DomainException, TargetedDecision}

import scala.concurrent.Future

trait PolicyDecisionPoint {
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

  def makeDecision(
    targets: Array[Target],
    attributes: Array[Attribute]
  )(implicit policyCollectionFetch: PolicyCollectionFetch): Future[Either[DomainException, Array[TargetedDecision]]]
}
