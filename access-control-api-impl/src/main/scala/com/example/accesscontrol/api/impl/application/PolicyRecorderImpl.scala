package com.example.accesscontrol.api.impl.application

import com.example.accesscontrol.api.impl.domain.{PolicyCollection, PolicyRecorder, PolicyRetrievalPoint}

import javax.inject.Inject

final case class PolicyRecorderImpl @Inject()(policyRetrievalPoint: PolicyRetrievalPoint) extends PolicyRecorder {
  def registerPolicyCollection(policyCollection: PolicyCollection): Unit = {
    policyRetrievalPoint.registerPolicyCollection(policyCollection)
  }
}
