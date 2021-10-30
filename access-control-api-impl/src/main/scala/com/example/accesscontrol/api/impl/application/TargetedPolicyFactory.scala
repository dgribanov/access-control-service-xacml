package com.example.accesscontrol.api.impl.application

import com.example.accesscontrol.api.impl.data.mapping.{
  PolicySetSerializableImpl,
  ObjectTypeTarget,
  ActionTypeTarget,
  AttributeTypeTarget
}
import com.example.accesscontrol.api.impl.domain.{
  PolicyCollection,
  Target,
  TargetType,
  Policy,
  TargetedPolicy
}

final class TargetedPolicyFactory {
  def createTargetedPolicy(target: Target, policyCollection: PolicyCollection): Option[TargetedPolicy] = {
    val policies = for {
      policySet <- policyCollection.policySets.asInstanceOf[Array[PolicySetSerializableImpl]]
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

case class TargetedPolicyImpl(target: Target, policy: Policy) extends TargetedPolicy
