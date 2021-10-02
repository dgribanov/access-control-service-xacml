package com.example.accesscontrol.api.impl.domain

import com.example.accesscontrol.rest.api.{Attribute, AttributeValue, AttributeValueBool, AttributeValueInt, AttributeValueString, Target}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object PolicyDecisionPoint {
  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future

  def makeDecision(
    targets: Array[Target],
    attributes: Array[Attribute],
    policyCollection: Future[Option[PolicyCollection]]
  ): Future[Option[Array[TargetedDecision]]] = {
    policyCollection.map({
      case Some(policyCollection) => Some(evaluate(targets, attributes, policyCollection))
      case None                   => None
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
    def targetMatcher(obj: WithTargetType): Boolean = {
      obj.target match {
        case ObjectTypeTarget(value: String) => value == objectType
        case ActionTypeTarget(value: String) => value == action
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
      case "eq" => (lOp: ExpressionValue[Any], rOp: ExpressionValue[Any])  => lOp equals rOp
      case "lt" => (lOp: ExpressionValue[Any], rOp: ExpressionValue[Any])  => lOp < rOp
      case "lte" => (lOp: ExpressionValue[Any], rOp: ExpressionValue[Any]) => lOp <= rOp
      case "gt" => (lOp: ExpressionValue[Any], rOp: ExpressionValue[Any])  => lOp > rOp
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
              case (d: Decision.Deny, _)                           => d
              case (_, d: Decision.Deny)                           => d
              case (d: Decision.Permit, _)                         => d
              case (_: Decision.Indeterminate, d: Decision.Permit) => d
            }
          )
    }
  }
}

/**
 * Inspired by scala.math.PartiallyOrdered idea for compare objects
 * but with more correct option result of compare methods
 */
sealed trait ExpressionValue[+T] {
  def equals[A >: T](obj: A): Option[Boolean]

  def tryCompareTo [B >: T](that: B): Option[Int]

  def < [B >: T](that: B): Option[Boolean] =
    (this tryCompareTo that) match {
      case Some(x) if x < 0 => Some(true)
      case Some(x) if x >= 0 => Some(false)
      case None => None
    }

  def > [B >: T](that: B): Option[Boolean] =
    (this tryCompareTo that) match {
      case Some(x) if x > 0 => Some(true)
      case Some(x) if x <= 0 => Some(false)
      case None => None
    }

  def <= [B >: T](that: B): Option[Boolean] =
    (this tryCompareTo that) match {
      case Some(x) if x <= 0 => Some(true)
      case Some(x) if x > 0 => Some(false)
      case None => None
    }

  def >= [B >: T](that: B): Option[Boolean] =
    (this tryCompareTo that) match {
      case Some(x) if x >= 0 => Some(true)
      case Some(x) if x < 0 => Some(false)
      case None => None
    }
}

object ExpressionValue {
  def apply(paramValue: ExpressionParameterValue, attributes: Array[Attribute]): ExpressionValue[Any] = {
    paramValue match {
      case AttributeParameterValue(id) => AttributeExpressionValue(id, attributes)
      case BoolParameterValue(value)   => BoolExpressionValue(value)
      case IntParameterValue(value)    => IntExpressionValue(value)
      case StringParameterValue(value) => StringExpressionValue(value)
    }
  }

  abstract case class AttributeExpressionValue(value: ExpressionValue[Any]) extends ExpressionValue[AttributeExpressionValue] {
    override def equals[A >: AttributeExpressionValue](obj: A): Option[Boolean] = {
      value equals obj
    }

    override def tryCompareTo[B >: AttributeExpressionValue](that: B): Option[Int] = None
  }

  abstract case class BoolExpressionValue(value: Boolean) extends ExpressionValue[BoolExpressionValue] {
    override def equals[A >: BoolExpressionValue](that: A): Option[Boolean] = {
      that match {
        case BoolExpressionValue(v) => Some(value == v) // compare Boolean and Boolean
        case IntExpressionValue     => None // don`t compare Boolean and Int
        case StringExpressionValue  => None // don`t compare Boolean and String
      }
    }

    override def tryCompareTo[B >: BoolExpressionValue](that: B): Option[Int] = None
  }

  abstract case class IntExpressionValue(value: Int) extends ExpressionValue[IntExpressionValue] {
    override def equals[A >: IntExpressionValue](obj: A): Option[Boolean] = {
      obj match {
        case BoolExpressionValue   => None // don`t compare Int and Boolean
        case IntExpressionValue(v) => Some(value == v) // compare Int and Int
        case StringExpressionValue => None // don`t compare Int and String
      }
    }

    override def tryCompareTo[B >: IntExpressionValue](that: B): Option[Int] = {
      that match {
        case BoolExpressionValue   => None // don`t compare Int and Boolean
        case IntExpressionValue(v) => Some(value - v) // compare Int and Int
        case StringExpressionValue => None // don`t compare Int and String
      }
    }
  }

  abstract case class StringExpressionValue(value: String) extends ExpressionValue[StringExpressionValue] {
    override def equals[A >: StringExpressionValue](obj: A): Option[Boolean] = {
      obj match {
        case BoolExpressionValue      => None // don`t compare String and Boolean
        case IntExpressionValue       => None // don`t compare String and Int
        case StringExpressionValue(v) => Some(value == v) // compare String and String
      }
    }

    override def tryCompareTo[B >: StringExpressionValue](that: B): Option[Int] = None
  }

  private case class EmptyExpressionValue() extends ExpressionValue[EmptyExpressionValue] {
    override def equals[A >: EmptyExpressionValue](obj: A): Option[Boolean] = None // don`t compare empty value and any other value

    override def tryCompareTo[B >: EmptyExpressionValue](that: B): Option[Int] = None // don`t compare empty value and any other value
  }

  object AttributeExpressionValue {
    def apply(id: String, attributes: Array[Attribute]): AttributeExpressionValue = {
      val value = attributes.foldRight[ExpressionValue[Any]](EmptyExpressionValue())(
        (attribute, value) => if (attribute.name == id) toExpressionValue(attribute.value) else value
      )
      new ExpressionValue.AttributeExpressionValue(value) {}
    }

    private def toExpressionValue(attributeValue: AttributeValue): ExpressionValue[Any] = {
      attributeValue match {
        case AttributeValueString(value) => StringExpressionValue(value)
        case AttributeValueBool(value)   => BoolExpressionValue(value)
        case AttributeValueInt(value)    => IntExpressionValue(value)
      }
    }
  }

  object BoolExpressionValue {
    def apply(value: Boolean): BoolExpressionValue = new ExpressionValue.BoolExpressionValue(value) {}
  }

  object IntExpressionValue {
    def apply(value: Int): IntExpressionValue = new ExpressionValue.IntExpressionValue(value) {}
  }

  object StringExpressionValue {
    def apply(value: String): StringExpressionValue = new ExpressionValue.StringExpressionValue(value) {}
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

abstract class TargetedDecision(val target: Target, val decision: Decision)
object TargetedDecision {
  def apply(target: Target, decision: Decision): TargetedDecision = new TargetedDecision(target, decision) {}
}
