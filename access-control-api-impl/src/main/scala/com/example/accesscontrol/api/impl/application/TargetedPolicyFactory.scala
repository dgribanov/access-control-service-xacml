package com.example.accesscontrol.api.impl.application

// todo remove wrong dependency
import com.example.accesscontrol.api.impl.data.mapping.{ActionTypeTarget, AttributeParameterValue, AttributeTypeTarget, BoolParameterValue, CompareCondition, CompositeCondition, IntParameterValue, ObjectTypeTarget, PolicySerializable, PolicySetSerializable, RuleSerializable, StringParameterValue, PolicyCollectionSerializable}
import com.example.accesscontrol.api.impl.domain.{AttributeParameterValueImpl, BoolParameterValueImpl, CompareConditionImpl, CompositeConditionImpl, Condition, ConditionImpl, ExpressionParameterValue, ExpressionParameterValueImpl, IntParameterValueImpl, Policy, PolicyCollection, PolicySet, PolicyImpl, Rule, RuleImpl, PolicySetImpl, PolicyCollectionImpl, StringParameterValueImpl, Target, TargetType, TargetedPolicy}

final class TargetedPolicyFactory {
  def createTargetedPolicy(target: Target, policyCollection: PolicyCollection): Option[TargetedPolicy] = {
    val policies = for {
      policySet <- PolicyCollectionConverter.convert(policyCollection).policySets
      if targetMatcher(target, policySet)
      policy <- policySet.policies
      if targetMatcher(target, policy)
    } yield policy

    policies match {
      case p: Array[_] if p.isEmpty     => None
      case p: Array[_] if p.length > 1  => None
      case p: Array[_] if p.length == 1 => Some(TargetedPolicyImpl(target, p(0)))
    }
  }

  private def targetMatcher(checkTarget: Target, obj: {val target: TargetType}): Boolean = {
    obj.target match {
      case ObjectTypeTarget(value: String) => value == checkTarget.objectType
      case ActionTypeTarget(value: String) => value == checkTarget.action
      case AttributeTypeTarget(_)          => false
    }
  }
}

object PolicyCollectionConverter {
  def convert: PartialFunction[PolicyCollection, PolicyCollectionImpl] = {
    case PolicyCollectionSerializable(pS) => PolicyCollectionImpl(pS map PolicySetConverter.convert)
  }
}

object PolicySetConverter {
  def convert: PartialFunction[PolicySet, PolicySetImpl] = {
    case PolicySetSerializable(t, cA, p) => PolicySetImpl(t, cA, p map PolicyConverter.convert)
  }
}

object PolicyConverter {
  def convert: PartialFunction[Policy, PolicyImpl] = {
    case PolicySerializable(t, cA, r) =>
      PolicyImpl(t, cA, r map RuleConverter.convert)
  }
}

object RuleConverter {
  def convert: PartialFunction[Rule, RuleImpl] = {
    case RuleSerializable(t, c, pE, nE) =>
      RuleImpl(t, ConditionConverter.convert(c), pE, nE)
  }
}

object ConditionConverter {
  def convert: PartialFunction[Condition, ConditionImpl] = {
    case CompareCondition(o, lOp, rOp) =>
      CompareConditionImpl(o, ExpressionParameterValueConverter.convert(lOp), ExpressionParameterValueConverter.convert(rOp))
    case CompositeCondition(p, lC, rC) =>
      CompositeConditionImpl(p, ConditionConverter.convert(lC), ConditionConverter.convert(rC))
  }
}

object ExpressionParameterValueConverter {
  def convert: PartialFunction[ExpressionParameterValue, ExpressionParameterValueImpl] = {
    case AttributeParameterValue(id) => AttributeParameterValueImpl(id)
    case BoolParameterValue(value)   => BoolParameterValueImpl(value)
    case IntParameterValue(value)    => IntParameterValueImpl(value)
    case StringParameterValue(value) => StringParameterValueImpl(value)
  }
}

case class TargetedPolicyImpl(target: Target, policy: Policy) extends TargetedPolicy
