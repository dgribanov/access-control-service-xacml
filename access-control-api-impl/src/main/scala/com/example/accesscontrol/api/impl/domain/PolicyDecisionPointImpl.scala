package com.example.accesscontrol.api.impl.domain

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

import com.example.accesscontrol.api.impl.application.PolicyDecisionPoint
import com.example.accesscontrol.api.impl.application.PolicyDecisionPoint._
import com.example.accesscontrol.api.impl.application.Decision

final case class PolicyDecisionPointImpl @Inject() (policyRetrievalPoint: PolicyRetrievalPoint) extends PolicyDecisionPoint {
  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future

  def makeDecision(
    targets: Array[Target],
    attributes: Array[Attribute]
  ): Future[Either[RuntimeException, Array[PolicyDecisionPoint.TargetedDecision]]] = {
    policyRetrievalPoint.buildPolicyCollection().map({
      case Right(policyCollection) => Right(evaluate(policyCollection)(targets, attributes))
      case Left(error)             => Left(error)
    })
  }

  private def evaluate(
    policyCollection: PolicyCollection
  )(implicit targets: Array[Target], attributes: Array[Attribute]): Array[PolicyDecisionPoint.TargetedDecision] = {
    for {
      target <- targets
      targetedDecision = computeTargetedDecision(target, policyCollection)
    } yield targetedDecision
  }

  private def fetchTargetedPolicy(checkTarget: Target, policyCollection: PolicyCollection): Option[TargetedPolicy] = {
    def targetMatcher(obj: {val target: TargetType}): Boolean = {
      obj.target match {
        case ObjectTypeTarget(value: String) => value == checkTarget.objectType
        case ActionTypeTarget(value: String) => value == checkTarget.action
        case AttributeTypeTarget(_)          => false
      }
    }

    val policies = policyCollection.policySets
      .filter(targetMatcher)
      .flatMap(
        _.policies.filter(targetMatcher)
      )

    policies match {
      case p: Array[Policy @unchecked] if p.isEmpty     => None
      case p: Array[Policy @unchecked] if p.length > 1  => None
      case p: Array[Policy @unchecked] if p.length == 1 => Some(TargetedPolicy(checkTarget, p(0)))
    }
  }

  private def computeTargetedDecision(
    target: Target,
    policyCollection: PolicyCollection
  )(implicit attributes: Array[Attribute]): TargetedDecision = {
    val targetedPolicy = fetchTargetedPolicy(target, policyCollection)
    targetedPolicy match {
      case None     => TargetedDecision(target, Future { Decisions.NonApplicable() })
      case Some(tp) => TargetedDecision(target, combineDecisions(computeDecisions(tp.policy.rules), tp.policy.combiningAlgorithm))
    }
  }

  private def computeDecisions(rules: Array[Rule])(implicit attributes: Array[Attribute]): Array[Future[Decision]] = {
    for {
      rule <- rules
      decision = computeDecision(
        checkCondition(rule.condition),
        {
          case true  => decisionTypeToDecision(rule.positiveEffect.decision)
          case false => decisionTypeToDecision(rule.negativeEffect.decision)
        }
      )
    } yield decision
  }

  private def computeDecision(resolution: Future[Option[Boolean]], f: Boolean => Decision): Future[Decision] = {
    resolution map {
      case Some(res) => f(res)
      case None      => Decisions.Indeterminate()
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
      case "Deny"   => Decisions.Deny()
      case "Permit" => Decisions.Permit()
    }
  }

  private def combineDecisions(decisions: Array[Future[Decision]], combiningAlgorithm: CombiningAlgorithm): Future[Decision] = {
    @tailrec
    def denyOverride(decisions: List[Decision], defaultDecision: Decision): Decision = {
      if (decisions == Nil) defaultDecision
      else if ((decisions.head: @unchecked) match {
        case _: Decisions.Deny          => true
        case _: Decisions.Indeterminate => true
        case _: Decisions.Permit        => false
      }) Decisions.Deny()
      else denyOverride(decisions.tail, Decisions.Permit())
    }

    @tailrec
    def permitOverride(decisions: List[Decision], defaultDecision: Decision): Decision = {
      if (decisions == Nil) defaultDecision
      else if ((decisions.head: @unchecked) match {
        case _: Decisions.Permit        => true
        case _: Decisions.Indeterminate => false
        case _: Decisions.Deny          => false
      }) Decisions.Permit()
      else permitOverride(decisions.tail, Decisions.Deny())
    }

    combiningAlgorithm match {
      case _: DenyOverride   => Future.sequence(decisions.toList) map {
        case d: List[Decision @unchecked] if d.nonEmpty => denyOverride(d, Decisions.NonApplicable())
        case d: List[Decision @unchecked] if d.isEmpty  => Decisions.NonApplicable()
      }
      case _: PermitOverride => Future.sequence(decisions.toList) map {
        case d: List[Decision @unchecked] if d.nonEmpty => permitOverride(d, Decisions.NonApplicable())
        case d: List[Decision @unchecked] if d.isEmpty  => Decisions.NonApplicable()
      }
    }
  }
}

// implement trait com.example.accesscontrol.api.impl.application.Decision
object Decisions {
  abstract case class Deny() extends Decision {
    override def toString: String = "Deny"
  }
  abstract case class Permit() extends Decision {
    override def toString: String = "Permit"
  }
  abstract case class Indeterminate() extends Decision {
    override def toString: String = "Indeterminate"
  }
  abstract case class NonApplicable() extends Decision {
    override def toString: String = "NonApplicable"
  }

  object Deny {
    def apply(): Deny = new Decisions.Deny {}
  }

  object Permit {
    def apply(): Permit = new Decisions.Permit {}
  }

  object Indeterminate {
    def apply(): Indeterminate = new Decisions.Indeterminate {}
  }

  object NonApplicable {
    def apply(): NonApplicable = new Decisions.NonApplicable {}
  }
}

abstract class TargetedPolicy(val target: Target, val policy: Policy)
object TargetedPolicy {
  def apply(target: Target, policy: Policy): TargetedPolicy = new TargetedPolicy(target, policy) {}
}

abstract class TargetedDecision(val target: Target, val decision: Future[Decision])
object TargetedDecision {
  def apply(target: Target, decision: Future[Decision]): TargetedDecision = new TargetedDecision(target, decision) {}
}
