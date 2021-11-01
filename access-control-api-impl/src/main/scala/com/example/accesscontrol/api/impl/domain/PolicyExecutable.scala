package com.example.accesscontrol.api.impl.domain

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

object PolicyExecutable {
  def convert: PartialFunction[Policy, PolicyExecutable] = {
    case p: Policy => PolicyExecutable(p.target, p.combiningAlgorithm, p.rules map RuleExecutable.convert)
  }
}

case class PolicyExecutable(target: TargetType, combiningAlgorithm: CombiningAlgorithms.Algorithm, rules: Array[RuleExecutable]) extends Policy {
  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future

  def makeDecision(attributes: Array[Attribute]): Future[Decision] = {
    combineDecisions(rules map (_.makeDecision(attributes)))
  }

  private def combineDecisions(decisions: Array[Future[Decision]]): Future[Decision] = {
    @tailrec
    def denyOverride(decisions: List[Decision], defaultDecision: Decision): Decision = {
      if (decisions == Nil) defaultDecision
      else if ((decisions.head: @unchecked) match {
        case _: Decisions.Deny          => true
        case _: Decisions.Indeterminate => true
        case _: Decisions.Permit        => false
      }) Decisions.Deny()
      else denyOverride(decisions.tail, Decisions.Permit())
    }

    @tailrec
    def permitOverride(decisions: List[Decision], defaultDecision: Decision): Decision = {
      if (decisions == Nil) defaultDecision
      else if ((decisions.head: @unchecked) match {
        case _: Decisions.Permit        => true
        case _: Decisions.Indeterminate => false
        case _: Decisions.Deny          => false
      }) Decisions.Permit()
      else permitOverride(decisions.tail, Decisions.Deny())
    }

    combiningAlgorithm match {
      case CombiningAlgorithms.DenyOverride => Future.sequence(decisions.toList) map {
        case d: List[Decision @unchecked] if d.nonEmpty => denyOverride(d, Decisions.NonApplicable())
        case d: List[Decision @unchecked] if d.isEmpty  => Decisions.NonApplicable()
      }
      case CombiningAlgorithms.PermitOverride => Future.sequence(decisions.toList) map {
        case d: List[Decision @unchecked] if d.nonEmpty => permitOverride(d, Decisions.NonApplicable())
        case d: List[Decision @unchecked] if d.isEmpty  => Decisions.NonApplicable()
      }
    }
  }
}
