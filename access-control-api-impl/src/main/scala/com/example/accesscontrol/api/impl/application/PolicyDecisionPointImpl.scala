package com.example.accesscontrol.api.impl.application

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

import com.example.accesscontrol.api.impl.domain.{
  Attribute,
  Decisions,
  PolicyDecisionPoint,
  PolicyExecutable,
  PolicyRetrievalPoint,
  Target,
  TargetType,
  TargetedDecision
}

final case class PolicyDecisionPointImpl @Inject()(policyRetrievalPoint: PolicyRetrievalPoint) extends PolicyDecisionPoint {
  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future

  def makeDecision(
    subject: String,
    id: String,
    targets: Array[Target],
    attributes: Array[Attribute]
  ): Future[List[TargetedDecision]] = {
    Future.sequence(
      targets.map(
        target => policyRetrievalPoint.fetchPolicy(
          subject,
          createTargetType(target.objectType),
          createTargetType(target.action),
        ).map({
          case Some(policy) =>
            TargetedDecision(
              target,
              PolicyExecutable.convert(policy).makeDecision(attributes)
            )
          case None =>
            TargetedDecision(
              target,
              Future {
                Decisions.NonApplicable()
              }
            )
        })
      ).toList
    )
  }

  private def createTargetType(targetType: String): TargetType = {
    new TargetType {
      override val value: String = targetType
    }
  }
}
