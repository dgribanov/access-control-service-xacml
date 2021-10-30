package com.example.accesscontrol.api.impl

import com.example.accesscontrol.api.impl.application.{PolicyDecisionPointImpl, TargetedPolicyFactory}
import com.example.accesscontrol.api.impl.data.mapping.PolicyRetrievalPointImpl
import com.example.accesscontrol.api.impl.data.storage.PolicyRepositoryImpl
import com.example.accesscontrol.api.impl.domain.{PolicyDecisionPoint, PolicyRepository, PolicyRetrievalPoint}
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

class AccessControlModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[PolicyDecisionPoint].to[PolicyDecisionPointImpl]
    bind[PolicyRetrievalPoint].to[PolicyRetrievalPointImpl]
    bind[PolicyRepository].to[PolicyRepositoryImpl]
    bind[TargetedPolicyFactory]
  }
}
