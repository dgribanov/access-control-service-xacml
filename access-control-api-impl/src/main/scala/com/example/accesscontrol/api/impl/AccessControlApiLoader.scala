package com.example.accesscontrol.api.impl

import com.example.accesscontrol.admin.ws.rest.api.AccessControlAdminWsService
import com.example.accesscontrol.api.impl.apirest.AccessControlRestApiService
import com.example.accesscontrol.api.impl.domain.{PolicyDecisionPoint, PolicyRecorder}
import com.example.accesscontrol.rest.api.AccessControlService

import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaClientComponents

import com.google.inject.Module
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents

class AccessControlApiLoader extends LagomApplicationLoader {
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
    with LagomKafkaClientComponents
    with AhcWSComponents {

  private val serviceInjector: ServiceInjector = new ServiceInjector
  private implicit val policyDecisionPoint: PolicyDecisionPoint = serviceInjector.inject[PolicyDecisionPoint]
  private implicit val policyRecorder: PolicyRecorder = serviceInjector.inject[PolicyRecorder]

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[AccessControlService](wire[AccessControlRestApiService])

  // Bind the AccessControlAdminWsService client
  lazy val accessControlAdminWsService: AccessControlAdminWsService =
    serviceClient.implement[AccessControlAdminWsService]
}