package com.example.accesscontrol.api.impl.domain

import com.example.accesscontrol.api.impl.infrastructure.TestPolicyRepositoryImpl
import com.example.accesscontrol.api.impl.BaseIntegrationSpec
import org.scalatest.Assertion
import com.example.accesscontrol.api.impl.application.PolicyDecisionPoint

import scala.concurrent.Future

class Target (val objectType: String, val objectId: Int, val action: String)
class Attribute (val name: String, val value: AttributeValue)
class AttributeValue (val value: Any)

class PolicyDecisionPointIntegrationSpec extends BaseIntegrationSpec {

  val policyRepository: PolicyRepository = new TestPolicyRepositoryImpl
  val policyRetrievalPoint: PolicyRetrievalPoint = new PolicyRetrievalPoint(policyRepository)
  val targetedPolicyFactory: TargetedPolicyFactory = new TargetedPolicyFactory

  info("Как сервис, управляющий продажей и арендой велосипедов, самокатов и скейтов, я хочу проверить доступ пользователя к основным действиям с моими товарами.")

  Feature("(0) Танки:") {
    Scenario("(0.1) Пользователь #1 хочет покататься на танке #1") {
      val targets = Array(new Target("tank", 1, "ride").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя ЕСТЬ право кататься на танке (он танкист)")
      val attributes = Array(new Attribute("permissionToRideBicycle", new AttributeValue(true)).asInstanceOf[PolicyDecisionPoint.Attribute])

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]]) }

      Then("<<< убеждаемся, что проверка доступа НЕ ПРИМЕНИМА для объекта `танк`, МЫ МИРНЫЕ ЛЮДИ, У НАС НЕТ ТАНКОВ!")
      assertDecision(decisions, nonApplicable = true)
    }
  }

  Feature("(1) Велосипеды:") {
    Scenario("(1.1) Пользователь #1 хочет покататься на велосипеде #1") {
      val targets = Array(new Target("bicycle", 1, "ride").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя ЕСТЬ право кататься на велосипеде")
      val attributes = Array(new Attribute("permissionToRideBicycle", new AttributeValue(true)).asInstanceOf[PolicyDecisionPoint.Attribute])

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]])}

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(1.2) Пользователь #2 хочет покататься на велосипеде #1") {
      val targets = Array(new Target("bicycle", 1, "ride").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя НЕТ права кататься на велосипеде")
      val attributes = Array(new Attribute("permissionToRideBicycle", new AttributeValue(false)).asInstanceOf[PolicyDecisionPoint.Attribute])

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]])}

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }

    Scenario("(1.3) Пользователь #3 хочет арендовать велосипед #1") {
      val targets = Array(new Target("bicycle", 1, "rent").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя ЕСТЬ право арендовать велосипед")
      Given("** пользователю НЕ ИСПОЛНИЛОСЬ 18 лет")
      Given("** пользователь хочет арендовать ТРЁХКОЛЁСНЫЙ велосипед")
      val attributes = Array(
        new Attribute("permissionToRentBicycle", new AttributeValue(true)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("personAge", new AttributeValue(17)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("bicycleType", new AttributeValue("tricycle")).asInstanceOf[PolicyDecisionPoint.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]])}

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(1.4) Пользователь #3 хочет арендовать велосипед #2") {
      val targets = Array(new Target("bicycle", 1, "rent").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя ЕСТЬ право арендовать велосипед")
      Given("** пользователю НЕ ИСПОЛНИЛОСЬ 18 лет")
      Given("** пользователь хочет арендовать ДВУХКОЛЁСНЫЙ велосипед")
      val attributes = Array(
        new Attribute("permissionToRentBicycle", new AttributeValue(true)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("personAge", new AttributeValue(17)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("bicycleType", new AttributeValue("two-wheeled")).asInstanceOf[PolicyDecisionPoint.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]])}

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }

    Scenario("(1.5) Пользователь #4 хочет арендовать велосипед #3") {
      val targets = Array(new Target("bicycle", 1, "rent").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя ЕСТЬ право арендовать велосипед")
      Given("** пользователю ИСПОЛНИЛОСЬ 18 лет")
      Given("** пользователь хочет арендовать ОДНОКОЛЁСНЫЙ велосипед")
      val attributes = Array(
        new Attribute("permissionToRentBicycle", new AttributeValue(true)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("personAge", new AttributeValue(18)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("bicycleType", new AttributeValue("unicycle")).asInstanceOf[PolicyDecisionPoint.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]])}

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(1.6) Пользователь #5 хочет арендовать велосипед #1") {
      val targets = Array(new Target("bicycle", 1, "rent").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя НЕТ ПРАВА арендовать велосипед")
      Given("** пользователю ИСПОЛНИЛОСЬ 18 лет")
      Given("** пользователь хочет арендовать ТРЁХКОЛЁСНЫЙ велосипед")
      val attributes = Array(
        new Attribute("permissionToRentBicycle", new AttributeValue(false)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("personAge", new AttributeValue(18)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("bicycleType", new AttributeValue("tricycle")).asInstanceOf[PolicyDecisionPoint.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]])}

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }
  }

  Feature("(2) Скейты:") {
    Scenario("(2.1) Пользователь #1 хочет покататься на скейте #1") {
      val targets = Array(new Target("skateboard", 1, "ride").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя ЕСТЬ право кататься на скейте")
      Given("** пользователь будет кататься на УЛИЦЕ")
      val attributes = Array(
        new Attribute("permissionToRideSkateboard", new AttributeValue(true)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("placeType", new AttributeValue("street")).asInstanceOf[PolicyDecisionPoint.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]]) }

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(2.2) Пользователь #2 хочет покататься на скейте #1") {
      val targets = Array(new Target("skateboard", 1, "ride").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя НЕТ права кататься на скейте")
      Given("** пользователь будет кататься на УЛИЦЕ")
      val attributes = Array(
        new Attribute("permissionToRideSkateboard", new AttributeValue(false)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("placeType", new AttributeValue("street")).asInstanceOf[PolicyDecisionPoint.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]]) }

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }

    Scenario("(2.3) Пользователь #1 хочет покататься на скейте #2") {
      val targets = Array(new Target("skateboard", 1, "ride").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя ЕСТЬ право кататься на скейте")
      Given("** пользователь будет кататься в ОФИСЕ")
      val attributes = Array(
        new Attribute("permissionToRideSkateboard", new AttributeValue(true)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("placeType", new AttributeValue("office")).asInstanceOf[PolicyDecisionPoint.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]]) }

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }
  }

  Feature("(3) Самокаты:") {
    Scenario("(3.1) Пользователь #1 хочет покататься на самокате #1") {
      val targets = Array(new Target("scooter", 1, "ride").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя ЕСТЬ право кататься на скейте")
      Given("** профессия пользователя - КУРЬЕР")
      val attributes = Array(
        new Attribute("permissionToRideScooter", new AttributeValue(true)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("profession", new AttributeValue("courier")).asInstanceOf[PolicyDecisionPoint.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]]) }

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(3.2) Пользователь #2 хочет покататься на самокате #1") {
      val targets = Array(new Target("scooter", 1, "ride").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя ЕСТЬ право кататься на скейте")
      Given("** профессия пользователя - ПРОГРАММИСТ")
      val attributes = Array(
        new Attribute("permissionToRideScooter", new AttributeValue(true)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("profession", new AttributeValue("programmer")).asInstanceOf[PolicyDecisionPoint.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]]) }

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(3.3) Пользователь #3 хочет покататься на самокате #1") {
      val targets = Array(new Target("scooter", 1, "ride").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя НЕТ права кататься на скейте")
      Given("** профессия пользователя - КУРЬЕР")
      val attributes = Array(
        new Attribute("permissionToRideScooter", new AttributeValue(false)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("profession", new AttributeValue("courier")).asInstanceOf[PolicyDecisionPoint.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]]) }

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(3.4) Пользователь #4 хочет покататься на самокате #1") {
      val targets = Array(new Target("scooter", 1, "ride").asInstanceOf[PolicyDecisionPoint.Target])

      Given("** у пользователя НЕТ права кататься на скейте")
      Given("** профессия пользователя - ПРОГРАММИСТ")
      val attributes = Array(
        new Attribute("permissionToRideScooter", new AttributeValue(false)).asInstanceOf[PolicyDecisionPoint.Attribute],
        new Attribute("profession", new AttributeValue("programmer")).asInstanceOf[PolicyDecisionPoint.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint, targetedPolicyFactory).makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]]) }

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }
  }
  private def assertDecision(
    decisions: Future[Either[RuntimeException, Array[PolicyDecisionPoint.TargetedDecision]]],
    deny: Boolean = false,
    permit: Boolean = false,
    indeterminate: Boolean = false,
    nonApplicable: Boolean = false
  ): Future[Assertion] = {
    (decisions map { checkless {
      case Right(targetedDecisions) => targetedDecisions(0) match {
        case td: TargetedDecision => td.decision
      }
    }
    }).flatten map { decision => assert(decision match {
      case _: Decisions.Deny          => deny
      case _: Decisions.Permit        => permit
      case _: Decisions.Indeterminate => indeterminate
      case _: Decisions.NonApplicable => nonApplicable
    })}
  }

  /**
   * Funny hack for suppression compiler standard warning 'match is not exhaustive'
   * when Scala `@unchecked` annotation not applicable
   * @see https://stackoverflow.com/questions/10507419/scala-where-to-put-the-unchecked-annotation-in-a-foreach
   */
  private def checkless[A,B](pf: PartialFunction[A,B]): A => B = pf: A => B
}
