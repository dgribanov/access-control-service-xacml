package com.example.accesscontrol.api.impl.domain

trait PolicyRecorder {
  def registerPolicyCollection(policyCollection: PolicyCollection): Unit
}
