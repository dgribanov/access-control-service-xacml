package com.example.accesscontrol.api.impl

import com.example.accesscontrol.api.impl.application.PolicyDecisionPoint
import com.example.accesscontrol.api.impl.domain.PolicyDecisionPointImpl

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

class AccessControlModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[PolicyDecisionPoint].toInstance(PolicyDecisionPointImpl)
  }
}
