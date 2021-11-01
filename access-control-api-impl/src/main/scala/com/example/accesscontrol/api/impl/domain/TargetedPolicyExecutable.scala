package com.example.accesscontrol.api.impl.domain

case class TargetedPolicyExecutable(target: Target, policy: Policy) extends TargetedPolicy
