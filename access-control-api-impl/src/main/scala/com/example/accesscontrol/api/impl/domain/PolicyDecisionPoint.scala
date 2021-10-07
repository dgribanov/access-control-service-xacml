package com.example.accesscontrol.api.impl.domain

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

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
  type PolicyCollectionFetch = () => Future[Either[DomainError, PolicyCollection]]

  def makeDecision(
    targets: Array[Target],
    attributes: Array[Attribute]
  )(implicit policyCollectionFetch: PolicyCollectionFetch): Future[Either[DomainError, Array[TargetedDecision]]] = {
    policyCollectionFetch().map({
      case Right(policyCollection) => Right(evaluate(policyCollection)(targets, attributes))
      case Left(error)             => Left(error)
    })
  }

  private def evaluate(
    policyCollection: PolicyCollection
  )(implicit targets: Array[Target], attributes: Array[Attribute]): Array[TargetedDecision] = {
    for {
      target <- targets
      policy <- fetchTargetedPolicies(target.objectType, target.action, policyCollection)
      decision = checkByRules(policy.rules, combineDecisionsFunc(policy.combiningAlgorithm))
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
    f: Future[List[Decision]] => Future[Decision]
  )(implicit attributes: Array[Attribute]): Future[Decision] = {
    val decisions = for {
      rule <- rules
      decision = computeDecision(
        checkCondition(rule.condition),
        {
          case true  => decisionTypeToDecision(rule.positiveEffect.decision)
          case false => decisionTypeToDecision(rule.negativeEffect.decision)
        }
      )
    } yield decision

    f(Future.sequence(decisions.toList))
  }

  private def computeDecision(resolution: Future[Option[Boolean]], f: Boolean => Decision): Future[Decision] = {
    resolution map {
      case Some(res) => f(res)
      case None      => Decision.Indeterminate()
    }
  }

  private def checkCondition(condition: Condition)(implicit attributes: Array[Attribute]): Future[Option[Boolean]] = {
    condition match {
      case CompareCondition(op, lOp, rOp)         => compareOperation(op, ExpressionValue(lOp), ExpressionValue(rOp))
      case CompositeCondition(pred, lCond, rCond) => composeConditions(pred, lCond, rCond)
    }
  }

  private def compareOperation(operationType: String, lOp: ExpressionValue[Any], rOp: ExpressionValue[Any]): Future[Option[Boolean]] = {
    operationType match {
      case "eq"  => Future { lOp equals rOp }
      case "lt"  => Future { lOp < rOp }
      case "lte" => Future { lOp <= rOp }
      case "gt"  => Future { lOp > rOp }
      case "gte" => Future { lOp >= rOp }
    }
  }

  private def composeConditions(predicate: String, lCond: Condition, rCond: Condition)(implicit attributes: Array[Attribute]): Future[Option[Boolean]] = {
    predicate match {
      case "and" => checkCondition(lCond) zip checkCondition(rCond) map {
        case (Some(lResult), Some(rResult)) => Some(lResult && rResult)
        case _                              => Some(false)
      }
      case "or"  => checkCondition(lCond) zip checkCondition(rCond) map {
        case (Some(lResult), Some(rResult)) => Some(lResult || rResult)
        case _                              => Some(false)
      }
    }
  }

  private def decisionTypeToDecision(decisionType: String): Decision = {
    decisionType match {
      case "Deny"   => Decision.Deny()
      case "Permit" => Decision.Permit()
    }
  }

  private def combineDecisionsFunc(combiningAlgorithm: CombiningAlgorithm): Future[List[Decision]] => Future[Decision] = {
    @tailrec
    def denyOverride(decisions: List[Decision], defaultDecision: Decision): Decision = {
      if (decisions == Nil) defaultDecision
      else if (decisions.head match {
        case _: Decision.Deny          => true
        case _: Decision.Indeterminate => true
        case _: Decision.Permit        => false
      }) Decision.Deny()
      else denyOverride(decisions.tail, Decision.Permit())
    }

    @tailrec
    def permitOverride(decisions: List[Decision], defaultDecision: Decision): Decision = {
      if (decisions == Nil) defaultDecision
      else if (decisions.head match {
        case _: Decision.Permit        => true
        case _: Decision.Indeterminate => false
        case _: Decision.Deny          => false
      }) Decision.Permit()
      else permitOverride(decisions.tail, Decision.Deny())
    }

    combiningAlgorithm match {
      case _: DenyOverride =>
        (decisions: Future[List[Decision]]) =>
          decisions map (denyOverride(_, Decision.Deny()))
      case _: PermitOverride =>
        (decisions: Future[List[Decision]]) =>
          decisions map (permitOverride(_, Decision.Deny()))
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

abstract class TargetedDecision(val target: PolicyDecisionPoint.Target, val decision: Future[Decision])
object TargetedDecision {
  def apply(target: PolicyDecisionPoint.Target, decision: Future[Decision]): TargetedDecision = new TargetedDecision(target, decision) {}
}
