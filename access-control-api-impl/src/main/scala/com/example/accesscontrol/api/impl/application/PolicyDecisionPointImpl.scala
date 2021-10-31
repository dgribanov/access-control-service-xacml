package com.example.accesscontrol.api.impl.application

import com.example.accesscontrol.api.impl.domain.{
  PolicyRetrievalPoint,
  PolicyDecisionPoint,
  Target,
  Attribute,
  TargetedDecision,
  Decisions,
  Decision,
  Condition,
  Predicates,
  Operations,
  EffectDecisions,
  ExpressionValue,
  CombiningAlgorithms
}
// todo remove dependency by data.mapping
import com.example.accesscontrol.api.impl.data.mapping.{CompareCondition, CompositeCondition}

import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

final case class PolicyDecisionPointImpl @Inject()(
  policyRetrievalPoint: PolicyRetrievalPoint,
  targetedPolicyFactory: TargetedPolicyFactory
) extends PolicyDecisionPoint {
  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future

  def makeDecision(
    targets: Array[Target],
    attributes: Array[Attribute]
  ): Future[Either[RuntimeException, Array[TargetedDecision]]] = {
    policyRetrievalPoint.buildPolicyCollection().map({
      case Right(policyCollection) => Right(
        targets.map(
          target => TargetedDecision(
            target,
            targetedPolicyFactory.createTargetedPolicy(target, policyCollection) match {
              case Some(tp) =>
                combineDecisions(
                  tp.policy.rules map (rule => computeDecision(
                    checkCondition(rule.condition)(attributes),
                    {
                      case true => decisionTypeToDecision(rule.positiveEffect.decision)
                      case false => decisionTypeToDecision(rule.negativeEffect.decision)
                    }
                  )),
                  tp.policy.combiningAlgorithm
                )
              case None => Future {
                Decisions.NonApplicable()
              }
            }
          )
        )
      )
      case Left(error) => Left(error)
    })
  }

  private def computeDecision(resolution: Future[Option[Boolean]], f: Boolean => Decision): Future[Decision] = {
    resolution map {
      case Some(res) => f(res)
      case None => Decisions.Indeterminate()
    }
  }

  private def checkCondition(
    condition: Condition
  )(implicit attributes: Array[Attribute]): Future[Option[Boolean]] = {
    def composeConditions(predicate: Predicates.Predicate, lCond: Condition, rCond: Condition)(implicit attributes: Array[Attribute]): Future[Option[Boolean]] = {
      predicate match {
        case Predicates.AND => checkCondition(lCond) zip checkCondition(rCond) map {
          case (Some(lResult), Some(rResult)) => Some(lResult && rResult)
          case _ => Some(false)
        }
        case Predicates.OR => checkCondition(lCond) zip checkCondition(rCond) map {
          case (Some(lResult), Some(rResult)) => Some(lResult || rResult)
          case _ => Some(false)
        }
      }
    }

    def compareOperation(operationType: Operations.Operation, lOp: ExpressionValue[Any], rOp: ExpressionValue[Any]): Future[Option[Boolean]] = {
      operationType match {
        case Operations.eq => Future {
          lOp equals rOp
        }
        case Operations.lt => Future {
          lOp < rOp
        }
        case Operations.lte => Future {
          lOp <= rOp
        }
        case Operations.gt => Future {
          lOp > rOp
        }
        case Operations.gte => Future {
          lOp >= rOp
        }
      }
    }

    condition match {
      case CompareCondition(op, lOp, rOp) => compareOperation(op, ExpressionValue(lOp), ExpressionValue(rOp))
      case CompositeCondition(pred, lCond, rCond) => composeConditions(pred, lCond, rCond)
    }
  }

  private def decisionTypeToDecision(decisionType: EffectDecisions.Decision): Decision = {
    decisionType match {
      case EffectDecisions.Deny => Decisions.Deny()
      case EffectDecisions.Permit => Decisions.Permit()
      case EffectDecisions.Indeterminate => Decisions.Indeterminate()
      case EffectDecisions.NonApplicable => Decisions.NonApplicable()
    }
  }

  private def combineDecisions(decisions: Array[Future[Decision]], combiningAlgorithm: CombiningAlgorithms.Algorithm): Future[Decision] = {
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
      case CombiningAlgorithms.DenyOverride => Future.sequence(decisions.toList) map {
        case d: List[Decision @unchecked] if d.nonEmpty => denyOverride(d, Decisions.NonApplicable())
        case d: List[Decision @unchecked] if d.isEmpty  => Decisions.NonApplicable()
      }
      case CombiningAlgorithms.PermitOverride => Future.sequence(decisions.toList) map {
        case d: List[Decision @unchecked] if d.nonEmpty => permitOverride(d, Decisions.NonApplicable())
        case d: List[Decision @unchecked] if d.isEmpty  => Decisions.NonApplicable()
      }
    }
  }
}
