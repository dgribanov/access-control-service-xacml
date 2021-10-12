package com.example.accesscontrol.api.impl.application

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.implicitConversions

import com.example.accesscontrol.api.impl.domain.{Decision, DomainException, PolicyDecisionPointImpl, PolicyRepository, PolicyRetrievalPoint, TargetedDecision}
import com.example.accesscontrol.api.impl.infrastructure.{PolicyRepositoryImpl, ServiceInjectorFactory}
import com.example.accesscontrol.rest.api.{AccessControlError, AccessControlRequest, AccessControlResponse, AccessControlService, AccessControlSuccessResponse, Attribute, ResultedDecision, Target}

/**
 * Implementation of the AccessControlService.
 */
class AccessControlRestApiService()(implicit ec: ExecutionContext) extends AccessControlService {

  override def healthcheck: ServiceCall[NotUsed, String] = ServiceCall {
    _ =>
      Future.successful("Hi from Access Control!")
  }

  override def check(subject: String, id: String): ServiceCall[AccessControlRequest, AccessControlResponse] = ServiceCall {
    request => {
      import net.codingwell.scalaguice.InjectorExtensions._

      val injector = ServiceInjectorFactory.buildServiceInjector()
      val policyDecisionPoint = injector.instance[PolicyDecisionPoint]

      policyDecisionPoint
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
  implicit val policyCollectionFetch: PolicyDecisionPointImpl.PolicyCollectionFetch =
    PolicyRetrievalPoint().buildPolicyCollection

  /**
   * Used for implicit conversion of Array[Target] to Array[PolicyDecisionPointImpl.Target] -
   * look at first parameter of PolicyDecisionPointImpl.makeDecision()
   */
  implicit def convertRequestTargetsToDomainTargets(
     requestTargets: Array[Target]
  ): Array[PolicyDecisionPointImpl.Target] = requestTargets.asInstanceOf[Array[PolicyDecisionPointImpl.Target]]

  /**
   * Used for implicit conversion of Array[Attribute] to Array[PolicyDecisionPointImpl.Attribute] -
   * look at second parameter of PolicyDecisionPointImpl.makeDecision()
   */
  implicit def convertRequestAttributesToDomainAttributes(
    requestAttributes: Array[Attribute]
  ): Array[PolicyDecisionPointImpl.Attribute] = requestAttributes.asInstanceOf[Array[PolicyDecisionPointImpl.Attribute]]

  private def convertToResponse(targetedDecisions: Either[DomainException, Array[TargetedDecision]]): AccessControlResponse = {
    targetedDecisions match {
      case Right(targetedDecisions) => toSuccessResponse(targetedDecisions)
      case Left(error)              => toError(error)
    }
  }

  private def toSuccessResponse(targetedDecisions: Array[TargetedDecision]): AccessControlSuccessResponse = {
    val decisions = targetedDecisions map createResultedDecision
    AccessControlSuccessResponse(decisions)
  }

  private def toError(error: DomainException): AccessControlError = AccessControlError(error.getMessage)

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
