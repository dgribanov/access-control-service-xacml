package com.example.accesscontrol.api.impl.application

import akka.NotUsed
import com.example.accesscontrol.api.impl.domain.{
  PolicyAdministrationPoint,
  PolicyDecisionPoint,
  TargetedDecision,
  Decision
}
import com.example.accesscontrol.rest.api.{
  AccessControlRequest,
  AccessControlResponse,
  AccessControlService,
  ResultedDecision
}
import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.{ExecutionContext, Future}

/**
 * Implementation of the AccessControlService.
 */
class AccessControlRestApiService()(implicit ec: ExecutionContext) extends AccessControlService {

  override def hello: ServiceCall[NotUsed, String] = ServiceCall {
    _ =>
      Future.successful("Hi from Access Control!")
  }

  override def check(subject: String, id: String): ServiceCall[AccessControlRequest, AccessControlResponse] = ServiceCall {
    request => {
      PolicyDecisionPoint
        .makeDecision(request.targets, request.attributes, PolicyAdministrationPoint.buildPolicyCollection())
        .map[AccessControlResponse](this.convertToResponse)
    }
  }

  private def convertToResponse(targetedDecisions: Option[Array[TargetedDecision]]): AccessControlResponse = {
    targetedDecisions match {
      case Some(targetedDecisions: Array[TargetedDecision]) => toResponse(targetedDecisions)
      case None                                             => toResponse(Array.empty)
    }
  }

  private def toResponse(targetedDecisions: Array[TargetedDecision]): AccessControlResponse = {
    val decisions = for {
        targetedDecision <- targetedDecisions
        decision = ResultedDecision(
          targetedDecision.target.objectType,
          targetedDecision.target.action,
          targetedDecision.decision match {
            case _: Decision.Deny          => "Deny"
            case _: Decision.Permit        => "Permit"
            case _: Decision.Indeterminate => "Indeterminate"
          },
        )
      } yield decision

    AccessControlResponse(decisions)
  }
}
