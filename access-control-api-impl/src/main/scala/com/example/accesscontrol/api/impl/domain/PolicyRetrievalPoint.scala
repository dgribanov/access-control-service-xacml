package com.example.accesscontrol.api.impl.domain

import play.api.libs.json.{Format, Json}

import scala.concurrent.Future

trait PolicyRetrievalPoint {
  case class PolicyCollectionParsingError(errorMessage: String) extends RuntimeException

  def buildPolicyCollection(): Future[Either[PolicyCollectionParsingError, PolicyCollection]]
}

trait TargetedPolicy {
  val target: Target
  val policy: Policy
}

trait PolicyCollection {
  val policySets: Array[_ <: PolicySet]
}

trait PolicySet {
  val target: TargetType
  val combiningAlgorithm: CombiningAlgorithms.Algorithm
  val policies: Array[_ <: Policy]
}

trait TargetType {
  val value: String
}

trait Policy {
  val target: TargetType
  val combiningAlgorithm: CombiningAlgorithms.Algorithm
  val rules: Array[_ <: Rule]
}

trait Rule {
  val target: TargetType
  val condition: Condition
  val positiveEffect: Effect
  val negativeEffect: Effect
}

// todo move to data
object CombiningAlgorithms extends Enumeration {
  type Algorithm = Value

  val DenyOverride, PermitOverride: Algorithm = Value

  implicit val format: Format[Algorithm] = Json.formatEnum(this)
}

trait Condition

// todo move to data
object Operations extends Enumeration {
  type Operation = Value

  val eq, lt, lte, gt, gte: Operation = Value

  implicit val format: Format[Operation] = Json.formatEnum(this)
}

// todo move to data
object Predicates extends Enumeration {
  type Predicate = Value

  val AND, OR: Predicate = Value

  implicit val format: Format[Predicate] = Json.formatEnum(this)
}

trait Effect {
  val decision: EffectDecisions.Decision
}

// todo move to data
object EffectDecisions extends Enumeration {
  type Decision = Value

  val Deny, Permit, Indeterminate, NonApplicable: Decision = Value

  implicit val format: Format[Decision] = Json.formatEnum(this)
}

trait ExpressionParameterValue
