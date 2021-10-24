package com.example.accesscontrol.api.impl

import com.example.accesscontrol.api.impl.data.storage.PolicyRepositoryImpl
import com.example.accesscontrol.api.impl.domain.{PolicyDecisionPoint, PolicyDecisionPointImpl, PolicyRepository, PolicyRetrievalPoint, TargetedPolicyFactory}
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

class AccessControlModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[PolicyDecisionPoint].to[PolicyDecisionPointImpl]
    bind[PolicyRetrievalPoint]
    bind[PolicyRepository].to[PolicyRepositoryImpl]
    bind[TargetedPolicyFactory]
  }
}
