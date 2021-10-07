package com.example.accesscontrol.api.impl.domain

import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AsyncFeatureSpec

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class Target (val objectType: String, val objectId: Int, val action: String)
class Attribute (val name: String, val value: AttributeValue)
class AttributeValue (val value: Any)

class PolicyDecisionPointSpec extends AsyncFeatureSpec with GivenWhenThen {

  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global

  implicit val policyCollectionFetch: PolicyDecisionPoint.PolicyCollectionFetch =
    PolicyAdministrationPoint.buildPolicyCollection

  info("Как сервис, управляющий продажей и арендой велосипедов, самокатов и скейтов, я хочу проверить доступ пользователя к основным действиям с моими товарами.")

  Feature("Велосипеды") {
    Scenario("Пользователь #1 хочет покататься на велосипеде #1") {
      val targets = Array(new Target("bicycle", 1, "ride").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя ЕСТЬ право кататься на велосипеде")
      val attributes = Array(new Attribute("permissionToRideBicycle", new AttributeValue(true)).asInstanceOf[PolicyDecisionPoint.Attribute])

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPoint.makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[Nothing, Array[TargetedDecision]]])}

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      (decisions map {
        case Right(targetedDecisions) => targetedDecisions(0) match {
          case td: TargetedDecision => td.decision
        }
      }).flatten map { decision => assert(decision.isInstanceOf[Decision.Permit])}
    }

    Scenario("Пользователь #2 хочет покататься на велосипеде #1") {
      val targets = Array(new Target("bicycle", 1, "ride").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя НЕТ права кататься на велосипеде")
      val attributes = Array(new Attribute("permissionToRideBicycle", new AttributeValue(false)).asInstanceOf[PolicyDecisionPoint.Attribute])

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPoint.makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[Nothing, Array[TargetedDecision]]])}

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      (decisions map {
        case Right(targetedDecisions) => targetedDecisions(0) match {
          case td: TargetedDecision => td.decision
        }
      }).flatten map { decision => assert(decision.isInstanceOf[Decision.Deny])}
    }
  }
}
