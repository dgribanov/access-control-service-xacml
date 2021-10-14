package com.example.accesscontrol.api.impl.application

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.implicitConversions
import com.google.inject.Module

import com.example.accesscontrol.api.impl.AccessControlModule
import com.example.accesscontrol.api.impl.ServiceInjector
import com.example.accesscontrol.rest.api.{
  AccessControlError,
  AccessControlRequest,
  AccessControlResponse,
  AccessControlService,
  AccessControlSuccessResponse,
  Attribute,
  ResultedDecision,
  Target
}

/**
 * Implementation of the AccessControlService.
 */
class AccessControlRestApiService()(implicit ec: ExecutionContext, module: Module = new AccessControlModule) extends AccessControlService {
  val policyDecisionPoint: PolicyDecisionPoint = ServiceInjector.inject[PolicyDecisionPoint]

  override def healthcheck: ServiceCall[NotUsed, String] = ServiceCall {
    _ =>
      Future.successful("Hi from Access Control!")
  }

  override def check(subject: String, id: String): ServiceCall[AccessControlRequest, AccessControlResponse] = ServiceCall {
    request => {
      policyDecisionPoint
        .makeDecision(request.targets, request.attributes)
        .map[AccessControlResponse](this.convertToResponse)
    }
  }

  /**
   * Used for implicit conversion of Array[Target] to Array[PolicyDecisionPointImpl.Target] -
   * look at first parameter of PolicyDecisionPointImpl.makeDecision()
   */
  implicit def convertRequestTargetsToDomainTargets(
     requestTargets: Array[Target]
  ): Array[PolicyDecisionPoint.Target] = requestTargets.asInstanceOf[Array[PolicyDecisionPoint.Target]]

  /**
   * Used for implicit conversion of Array[Attribute] to Array[PolicyDecisionPointImpl.Attribute] -
   * look at second parameter of PolicyDecisionPointImpl.makeDecision()
   */
  implicit def convertRequestAttributesToDomainAttributes(
    requestAttributes: Array[Attribute]
  ): Array[PolicyDecisionPoint.Attribute] = requestAttributes.asInstanceOf[Array[PolicyDecisionPoint.Attribute]]

  private def convertToResponse(targetedDecisions: Either[RuntimeException, Array[PolicyDecisionPoint.TargetedDecision]]): AccessControlResponse = {
    targetedDecisions match {
      case Right(targetedDecisions) => toSuccessResponse(targetedDecisions)
      case Left(error)              => toError(error)
    }
  }

  private def toSuccessResponse(targetedDecisions: Array[PolicyDecisionPoint.TargetedDecision]): AccessControlSuccessResponse = {
    val decisions = targetedDecisions map createResultedDecision
    AccessControlSuccessResponse(decisions)
  }

  private def toError(error: RuntimeException): AccessControlError = AccessControlError(error.getMessage)

  private def createResultedDecision(targetedDecision: PolicyDecisionPoint.TargetedDecision): ResultedDecision = {
    // block main thread while decision is completed
    val decision = Await.result(targetedDecision.decision, 10.seconds)

    ResultedDecision(
      targetedDecision.target.objectType,
      targetedDecision.target.action,
      decision.toString,
    )
  }
}
