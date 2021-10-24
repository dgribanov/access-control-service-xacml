package com.example.accesscontrol.api.impl.application

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.implicitConversions
import com.example.accesscontrol.api.impl.ServiceInjector
import com.example.accesscontrol.api.impl.domain.{Attribute, AttributeValue, PolicyDecisionPoint, Target, TargetedDecision}
import com.example.accesscontrol.rest.api.{
  AccessControlError => ApiAccessControlError,
  AccessControlRequest => ApiAccessControlRequest,
  AccessControlResponse => ApiAccessControlResponse,
  AccessControlService => ApiAccessControlService,
  AccessControlSuccessResponse => ApiAccessControlSuccessResponse,
  Attribute => ApiAttribute,
  ResultedDecision => ApiResultedDecision,
  Target => ApiTarget
}

class TargetImpl (val objectType: String, val objectId: Int, val action: String) extends Target
class AttributeImpl (val name: String, val value: AttributeValue) extends Attribute
class AttributeValueImpl (val value: Any) extends AttributeValue

/**
 * Implementation of the AccessControlService.
 */
class AccessControlRestApiService()(implicit ec: ExecutionContext, policyDecisionPoint: PolicyDecisionPoint) extends ApiAccessControlService {

  override def healthcheck: ServiceCall[NotUsed, String] = ServiceCall {
    _ =>
      Future.successful("Hi from Access Control!")
  }

  override def check(subject: String, id: String): ServiceCall[ApiAccessControlRequest, ApiAccessControlResponse] = ServiceCall {
    request => {
      policyDecisionPoint
        .makeDecision(request.targets, request.attributes)
        .map[ApiAccessControlResponse](this.convertToResponse)
    }
  }

  /**
   * Used for implicit conversion of Array[Target] to Array[PolicyDecisionPointImpl.Target] -
   * look at first parameter of PolicyDecisionPointImpl.makeDecision()
   */
  implicit def convertRequestTargetsToDomainTargets(
     requestTargets: Array[ApiTarget]
  ): Array[Target] = requestTargets.map(t => new TargetImpl(t.objectType, t.objectId, t.action)).asInstanceOf[Array[Target]]

  /**
   * Used for implicit conversion of Array[Attribute] to Array[PolicyDecisionPointImpl.Attribute] -
   * look at second parameter of PolicyDecisionPointImpl.makeDecision()
   */
  implicit def convertRequestAttributesToDomainAttributes(
    requestAttributes: Array[ApiAttribute]
  ): Array[Attribute] = requestAttributes.map(a => new AttributeImpl(a.name, new AttributeValueImpl(a.value.value))).asInstanceOf[Array[Attribute]]

  private def convertToResponse(targetedDecisions: Either[RuntimeException, Array[TargetedDecision]]): ApiAccessControlResponse = {
    targetedDecisions match {
      case Right(targetedDecisions) => toSuccessResponse(targetedDecisions)
      case Left(error)              => toError(error)
    }
  }

  private def toSuccessResponse(targetedDecisions: Array[TargetedDecision]): ApiAccessControlSuccessResponse = {
    val decisions = targetedDecisions map createResultedDecision
    ApiAccessControlSuccessResponse(decisions)
  }

  private def toError(error: RuntimeException): ApiAccessControlError = ApiAccessControlError(error.getMessage)

  private def createResultedDecision(targetedDecision: TargetedDecision): ApiResultedDecision = {
    // block main thread while decision is completed
    // todo think more about non block conversation
    val decision = Await.result(targetedDecision.decision, 10.seconds)

    ApiResultedDecision(
      targetedDecision.target.objectType,
      targetedDecision.target.action,
      decision.toString,
    )
  }
}
