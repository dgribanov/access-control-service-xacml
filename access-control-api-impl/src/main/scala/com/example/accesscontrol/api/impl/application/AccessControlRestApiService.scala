package com.example.accesscontrol.api.impl.application

import akka.NotUsed
import com.example.accesscontrol.api.impl.domain.{Decision, DomainError, PolicyDecisionPoint, PolicyRepository, PolicyRetrievalPoint, TargetedDecision}
import com.example.accesscontrol.api.impl.infrastructure.PolicyRepositoryImpl
import com.example.accesscontrol.rest.api.{AccessControlError, AccessControlRequest, AccessControlResponse, AccessControlService, AccessControlSuccessResponse, Attribute, ResultedDecision, Target}
import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.implicitConversions

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
        .makeDecision(request.targets, request.attributes)
        .map[AccessControlResponse](this.convertToResponse)
    }
  }

  /**
   * Used as implicit parameter for PolicyRetrievalPoint()
   */
  implicit val policyRepository: PolicyRepository = new PolicyRepositoryImpl

  /**
   * Used as implicit parameter for PolicyDecisionPoint.makeDecision()
   */
  implicit val policyCollectionFetch: PolicyDecisionPoint.PolicyCollectionFetch =
    PolicyRetrievalPoint().buildPolicyCollection

  /**
   * Used for implicit conversion of Array[Target] to Array[PolicyDecisionPoint.Target] -
   * look at first parameter of PolicyDecisionPoint.makeDecision()
   */
  implicit def convertRequestTargetsToDomainTargets(
     requestTargets: Array[Target]
  ): Array[PolicyDecisionPoint.Target] = requestTargets.asInstanceOf[Array[PolicyDecisionPoint.Target]]

  /**
   * Used for implicit conversion of Array[Attribute] to Array[PolicyDecisionPoint.Attribute] -
   * look at second parameter of PolicyDecisionPoint.makeDecision()
   */
  implicit def convertRequestAttributesToDomainAttributes(
    requestAttributes: Array[Attribute]
  ): Array[PolicyDecisionPoint.Attribute] = requestAttributes.asInstanceOf[Array[PolicyDecisionPoint.Attribute]]

  private def convertToResponse(targetedDecisions: Either[DomainError, Array[TargetedDecision]]): AccessControlResponse = {
    targetedDecisions match {
      case Right(targetedDecisions) => toSuccessResponse(targetedDecisions)
      case Left(error)              => toError(error)
    }
  }

  private def toSuccessResponse(targetedDecisions: Array[TargetedDecision]): AccessControlSuccessResponse = {
    val decisions = targetedDecisions map createResultedDecision
    AccessControlSuccessResponse(decisions)
  }

  private def toError(error: DomainError): AccessControlError = AccessControlError(error.errorMessage)

  private def createResultedDecision(targetedDecision: TargetedDecision): ResultedDecision = {
    // block main thread while decision is completed
    val decision = Await.result(targetedDecision.decision, 10.seconds)

    ResultedDecision(
      targetedDecision.target.objectType,
      targetedDecision.target.action,
      decision match {
        case _: Decision.Deny          => "Deny"
        case _: Decision.Permit        => "Permit"
        case _: Decision.Indeterminate => "Indeterminate"
        case _: Decision.NonApplicable => "NonApplicable"
      },
    )
  }
}
