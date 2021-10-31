package com.example.accesscontrol.api.impl.domain

import scala.concurrent.Future

trait Decision
object Decisions {
  abstract case class Deny() extends Decision {
    override def toString: String = "Deny"
  }
  abstract case class Permit() extends Decision {
    override def toString: String = "Permit"
  }
  abstract case class Indeterminate() extends Decision {
    override def toString: String = "Indeterminate"
  }
  abstract case class NonApplicable() extends Decision {
    override def toString: String = "NonApplicable"
  }

  object Deny {
    def apply(): Deny = new Decisions.Deny {}
  }

  object Permit {
    def apply(): Permit = new Decisions.Permit {}
  }

  object Indeterminate {
    def apply(): Indeterminate = new Decisions.Indeterminate {}
  }

  object NonApplicable {
    def apply(): NonApplicable = new Decisions.NonApplicable {}
  }
}

abstract class TargetedDecision(val target: Target, val decision: Future[Decision])
object TargetedDecision {
  def apply(target: Target, decision: Future[Decision]): TargetedDecision = new TargetedDecision(target, decision) {}
}
