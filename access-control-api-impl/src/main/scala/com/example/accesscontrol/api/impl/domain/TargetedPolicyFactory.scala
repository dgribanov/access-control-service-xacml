package com.example.accesscontrol.api.impl.domain

final class TargetedPolicyFactory {
  def createTargetedPolicy(target: Target, policyCollection: PolicyCollection): Option[TargetedPolicy] = {
    val policies = policyCollection.policySets
      .filter(targetMatcher(target, _))
      .flatMap(
        _.policies.filter(targetMatcher(target, _))
      )

    policies match {
      case p: Array[Policy @unchecked] if p.isEmpty     => None
      case p: Array[Policy @unchecked] if p.length > 1  => None
      case p: Array[Policy @unchecked] if p.length == 1 => Some(TargetedPolicy(target, p(0)))
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

abstract class TargetedPolicy(val target: Target, val policy: Policy)
object TargetedPolicy {
  def apply(target: Target, policy: Policy): TargetedPolicy = new TargetedPolicy(target, policy) {}
}
