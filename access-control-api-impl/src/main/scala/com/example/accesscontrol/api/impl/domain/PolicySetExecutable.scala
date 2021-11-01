package com.example.accesscontrol.api.impl.domain

object PolicySetExecutable {
  def convert: PartialFunction[PolicySet, PolicySetExecutable] = {
    case ps: PolicySet => PolicySetExecutable(ps.target, ps.combiningAlgorithm, ps.policies map PolicyExecutable.convert)
  }
}

case class PolicySetExecutable(target: TargetType, combiningAlgorithm: CombiningAlgorithms.Algorithm, policies: Array[PolicyExecutable]) extends PolicySet
