package com.example.accesscontrol.api.impl.application

import com.example.accesscontrol.api.impl.domain.{
  PolicyRetrievalPoint,
  PolicyDecisionPoint,
  Target,
  Attribute,
  TargetedDecision,
  Decisions,
  PolicyExecutable
}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

final case class PolicyDecisionPointImpl @Inject()(
  policyRetrievalPoint: PolicyRetrievalPoint,
  targetedPolicyFactory: TargetedPolicyFactory
) extends PolicyDecisionPoint {
  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future

  def makeDecision(
    targets: Array[Target],
    attributes: Array[Attribute]
  ): Future[Either[PolicyCollectionFetchingError, Array[TargetedDecision]]] = {
    policyRetrievalPoint.fetchPolicyCollection().map({
      case Some(policyCollection) => Right(
        targets.map(
          target => TargetedDecision(
            target,
            targetedPolicyFactory.createTargetedPolicy(target, policyCollection) match {
              case Some(tp) => tp.policy match {
                case p: PolicyExecutable => p.makeDecision(attributes)
              }
              case None => Future {
                Decisions.NonApplicable()
              }
            }
          )
        )
      )
      case None => Left(PolicyCollectionFetchingError("Can`t fetch policy collection"))
    })
  }
}
