package com.example.accesscontrol.api.impl

import com.example.accesscontrol.api.impl.apirest.AccessControlRestApiService
import com.example.accesscontrol.api.impl.domain.PolicyDecisionPoint
import com.example.accesscontrol.rest.api.AccessControlService
import com.google.inject.Module
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.softwaremill.macwire._

class AccessControlLoader extends LagomApplicationLoader {
  private implicit val module: Module = new AccessControlModule

  override def load(context: LagomApplicationContext): LagomApplication =
    new AccessControlApiApplication(context) {
      override def serviceLocator: NoServiceLocator.type = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new AccessControlApiApplication(context) with LagomDevModeComponents

  override def describeService: Some[Descriptor] = Some(readDescriptor[AccessControlService])
}

abstract class AccessControlApiApplication(context: LagomApplicationContext)(private implicit val module: Module)
  extends LagomApplication(context)
    with AhcWSComponents {
  private val serviceInjector: ServiceInjector = new ServiceInjector
  private implicit val policyDecisionPoint: PolicyDecisionPoint = serviceInjector.inject[PolicyDecisionPoint]

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[AccessControlService](wire[AccessControlRestApiService])
}