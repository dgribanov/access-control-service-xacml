package com.example.accesscontrol.api.impl.domain

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object PolicyDecisionPoint {
  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future

  type Target = {
    val objectType: String
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
  )(implicit policyCollectionFetch: () => Future[Either[DomainError, PolicyCollection]]):
    Future[Either[DomainError, Array[TargetedDecision]]] =
  {
    policyCollectionFetch().map({
      case Right(policyCollection) => Right(evaluate(targets, attributes, policyCollection))
      case Left(error)             => Left(error)
    })
  }

  private def evaluate(
    targets: Array[Target],
    attributes: Array[Attribute],
    policyCollection: PolicyCollection
  ): Array[TargetedDecision] = {
    for {
      target <- targets
      policy <- fetchTargetedPolicies(target.objectType, target.action, policyCollection)
      decision = checkByRules(policy.rules, attributes, combineDecisionsFunc(policy.combiningAlgorithm))
    } yield TargetedDecision(target, decision)
  }

  private def fetchTargetedPolicies(objectType: String, action: String, policyCollection: PolicyCollection): Array[Policy] = {
    def targetMatcher(obj: {val target: TargetType}): Boolean = {
      obj.target match {
        case ObjectTypeTarget(value: String) => value == objectType
        case ActionTypeTarget(value: String) => value == action
        case AttributeTypeTarget(_)          => false
      }
    }

    policyCollection.policySets
      .filter(targetMatcher)
      .flatMap(
        _.policies.filter(targetMatcher)
      )
  }

  private def checkByRules(
    rules: Array[Rule],
    attributes: Array[Attribute],
    f: Array[Decision] => Decision
  ): Decision = {
    val decisions = for {
      rule <- rules
      decision = computeDecision(
        checkCondition(rule.condition, attributes),
        conditionResolutionToDecision(
          decisionTypeToDecision(rule.positiveEffect.decision),
          decisionTypeToDecision(rule.negativeEffect.decision)
        )
      )
    } yield decision

    f(decisions)
  }

  private def computeDecision(resolution: Option[Boolean], f: Boolean => Decision): Decision = {
    resolution match {
      case Some(resolution) => f(resolution)
      case None             => Decision.Indeterminate()
    }
  }

  private def checkCondition(condition: Condition, attributes: Array[Attribute]): Option[Boolean] = {
    condition match {
      case CompareCondition(op, lOp, rOp) => compareOperation(op)(ExpressionValue(lOp, attributes), ExpressionValue(rOp, attributes))
    }
  }

  private def compareOperation(operationType: String): (ExpressionValue[Any], ExpressionValue[Any]) => Option[Boolean] = {
    operationType match {
      case "eq"  => (lOp: ExpressionValue[Any], rOp: ExpressionValue[Any]) => lOp equals rOp
      case "lt"  => (lOp: ExpressionValue[Any], rOp: ExpressionValue[Any]) => lOp < rOp
      case "lte" => (lOp: ExpressionValue[Any], rOp: ExpressionValue[Any]) => lOp <= rOp
      case "gt"  => (lOp: ExpressionValue[Any], rOp: ExpressionValue[Any]) => lOp > rOp
      case "gte" => (lOp: ExpressionValue[Any], rOp: ExpressionValue[Any]) => lOp >= rOp
    }
  }

  private def conditionResolutionToDecision(positiveEffectDecision: Decision, negativeEffectDecision: Decision): Boolean => Decision = {
    case true => positiveEffectDecision
    case false => negativeEffectDecision
  }

  private def decisionTypeToDecision(decisionType: String): Decision = {
    decisionType match {
      case "Deny"   => Decision.Deny()
      case "Permit" => Decision.Permit()
    }
  }

  private def combineDecisionsFunc(combiningAlgorithm: CombiningAlgorithm): Array[Decision] => Decision = {
    combiningAlgorithm match {
      case DenyOverride(_) =>
        (decisions: Array[Decision]) =>
          decisions.foldRight[Decision](Decision.Indeterminate())(
            (decision, combinedDecision) => (decision, combinedDecision) match {
              case (d: Decision.Deny, _)                                  => d
              case (_, d: Decision.Deny)                                  => d
              case (d: Decision.Permit, _)                                => d
              case (_: Decision.Indeterminate, d: Decision.Permit)        => d
              case (_: Decision.Indeterminate, d: Decision.Indeterminate) => d
            }
          )
    }
  }
}

sealed trait Decision
object Decision {
  abstract case class Deny() extends Decision
  abstract case class Permit() extends Decision
  abstract case class Indeterminate() extends Decision

  object Deny {
    def apply(): Deny = new Decision.Deny {}
  }

  object Permit {
    def apply(): Permit = new Decision.Permit {}
  }

  object Indeterminate {
    def apply(): Indeterminate = new Decision.Indeterminate {}
  }
}

abstract class TargetedDecision(val target: PolicyDecisionPoint.Target, val decision: Decision)
object TargetedDecision {
  def apply(target: PolicyDecisionPoint.Target, decision: Decision): TargetedDecision = new TargetedDecision(target, decision) {}
}
