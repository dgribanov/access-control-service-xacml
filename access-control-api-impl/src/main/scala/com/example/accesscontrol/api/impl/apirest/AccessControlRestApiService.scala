package com.example.accesscontrol.api.impl.apirest

import com.example.accesscontrol.admin.ws.rest.api.{
  AccessControlAdminWsService,
  PolicyEvent,
  PolicyCollectionRegisteredEvent,
  PolicyCollection => AdminPolicyCollection
}
import com.example.accesscontrol.api.impl.domain.{
  PolicyRecorder,
  Attribute,
  AttributeValue,
  PolicyDecisionPoint,
  Target,
  TargetedDecision,
  PolicyCollection
}
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

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.implicitConversions
import akka.Done
import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.lightbend.lagom.scaladsl.api.ServiceCall

class TargetImpl (val objectType: String, val objectId: Int, val action: String) extends Target
class AttributeImpl (val name: String, val value: AttributeValue) extends Attribute
class AttributeValueImpl (val value: Any) extends AttributeValue

/**
 * Implementation of the AccessControlService.
 */
class AccessControlRestApiService(
  accessControlAdminWsService: AccessControlAdminWsService
)(
  implicit ec: ExecutionContext,
  policyDecisionPoint: PolicyDecisionPoint,
  policyRecorder: PolicyRecorder
) extends ApiAccessControlService {
  handlePolicyEvents()

  override def healthcheck: ServiceCall[NotUsed, String] = ServiceCall {
    _ =>
      Future.successful("Hi from Access Control!")
  }

  override def check(subject: String, id: String): ServiceCall[ApiAccessControlRequest, ApiAccessControlResponse] = ServiceCall {
    request => {
      policyDecisionPoint
        .makeDecision(subject, id, request.targets, request.attributes)
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

  private def convertToResponse(targetedDecisions: List[TargetedDecision]): ApiAccessControlResponse = {
    toSuccessResponse(targetedDecisions)
  }

  private def toSuccessResponse(targetedDecisions: List[TargetedDecision]): ApiAccessControlSuccessResponse = {
    val decisions = targetedDecisions map createResultedDecision
    ApiAccessControlSuccessResponse(decisions.toArray)
  }

  private def toError(error: RuntimeException): ApiAccessControlError = ApiAccessControlError(error.getMessage)

  private def createResultedDecision(targetedDecision: TargetedDecision): ApiResultedDecision = {
    // block main thread while decision is completed
    // todo think more about non block conversation
    val decision = Await.result(targetedDecision.decision, 1.nanosecond)

    ApiResultedDecision(
      targetedDecision.target.objectType,
      targetedDecision.target.action,
      decision.toString,
    )
  }

  private def handlePolicyEvents(): Unit =
    accessControlAdminWsService.policyEventsTopic.subscribe.atLeastOnce(Flow[PolicyEvent].map { event =>
      event match {
        case PolicyCollectionRegisteredEvent(policyCollection) => policyRecorder.registerPolicyCollection(policyCollection)
      }
      Done
    })

  implicit def convertAdminPolicyCollectionToDomainPolicyCollection(
    adminPolicyCollection: AdminPolicyCollection
  ): PolicyCollection = adminPolicyCollection.asInstanceOf[PolicyCollection]
}
