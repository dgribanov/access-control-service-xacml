package com.example.accesscontrol.api.impl.domain

import scala.concurrent.{ExecutionContext, Future}

case class RuleImpl(
  target: TargetType,
  condition: Condition,
  positiveEffect: Effect,
  negativeEffect: Effect
) extends Rule {
  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future

  def makeDecision(attributes: Array[Attribute]): Future[Decision] = {
    computeDecision(
      checkCondition(condition)(attributes),
      {
        case true  => decisionTypeToDecision(positiveEffect.decision)
        case false => decisionTypeToDecision(negativeEffect.decision)
      }
    )
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
      case CompareConditionImpl(op, lOp, rOp)         => compareOperation(op, ExpressionValue(lOp), ExpressionValue(rOp))
      case CompositeConditionImpl(pred, lCond, rCond) => composeConditions(pred, lCond, rCond)
    }
  }

  private def decisionTypeToDecision(decisionType: EffectDecisions.Decision): Decision = {
    decisionType match {
      case EffectDecisions.Deny          => Decisions.Deny()
      case EffectDecisions.Permit        => Decisions.Permit()
      case EffectDecisions.Indeterminate => Decisions.Indeterminate()
      case EffectDecisions.NonApplicable => Decisions.NonApplicable()
    }
  }
}

abstract class ConditionImpl extends Condition
case class CompareConditionImpl(operation: Operations.Operation, leftOperand: ExpressionParameterValueImpl, rightOperand: ExpressionParameterValueImpl) extends ConditionImpl {

}

case class CompositeConditionImpl(predicate: Predicates.Predicate, leftCondition: ConditionImpl, rightCondition: ConditionImpl) extends ConditionImpl {

}

abstract class ExpressionParameterValueImpl extends ExpressionParameterValue
case class AttributeParameterValueImpl(id: String) extends ExpressionParameterValueImpl
case class BoolParameterValueImpl(value: Boolean) extends ExpressionParameterValueImpl
case class IntParameterValueImpl(value: Int) extends ExpressionParameterValueImpl
case class StringParameterValueImpl(value: String) extends ExpressionParameterValueImpl
