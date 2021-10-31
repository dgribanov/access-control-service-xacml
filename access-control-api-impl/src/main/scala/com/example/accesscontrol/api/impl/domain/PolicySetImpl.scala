package com.example.accesscontrol.api.impl.domain

case class PolicySetImpl(target: TargetType, combiningAlgorithm: CombiningAlgorithms.Algorithm, policies: Array[PolicyImpl]) extends PolicySet
