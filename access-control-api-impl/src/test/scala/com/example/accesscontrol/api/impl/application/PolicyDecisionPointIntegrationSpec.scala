package com.example.accesscontrol.api.impl.application

import com.example.accesscontrol.api.impl.data.storage.TestPolicyRepositoryImpl
import com.example.accesscontrol.api.impl.domain.{
  Attribute,
  AttributeValue,
  Decisions,
  PolicyRepository,
  PolicyRetrievalPoint,
  Target,
  TargetedDecision
}
import com.example.accesscontrol.api.impl.BaseIntegrationSpec
import com.example.accesscontrol.api.impl.data.mapping.PolicyRetrievalPointImpl

import org.scalatest.Assertion

import scala.concurrent.Future

class TestTarget (val objectType: String, val objectId: Int, val action: String) extends Target
class TestAttribute (val name: String, val value: AttributeValue) extends Attribute
class TestAttributeValue (val value: Any) extends AttributeValue

class PolicyDecisionPointIntegrationSpec extends BaseIntegrationSpec {

  val policyRepository: PolicyRepository = new TestPolicyRepositoryImpl
  val policyRetrievalPoint: PolicyRetrievalPoint = new PolicyRetrievalPointImpl()
  policyRetrievalPoint.buildPolicyCollection(policyRepository)

  val subject: String = "user"
  val userId: String = "1"

  info("Как сервис, управляющий продажей и арендой велосипедов, самокатов и скейтов, я хочу проверить доступ пользователя к основным действиям с моими товарами.")

  Feature("(0) Вертолёт:") {
    Scenario("(0.1) Пользователь #1 хочет полетать на вертолёте") {
      val targets = Array(new TestTarget("tank", 1, "ride").asInstanceOf[Target])

      Given("** у пользователя ЕСТЬ право летать на вертолёте")
      val attributes = Array(new TestAttribute("permissionToFlyHelicopter", new TestAttributeValue(true)).asInstanceOf[Attribute])

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что проверка доступа НЕ ПРИМЕНИМА для объекта `вертолёт`")
      assertDecision(decisions, nonApplicable = true)
    }
  }

  Feature("(1) Велосипеды:") {
    Scenario("(1.1) Пользователь #1 хочет покататься на велосипеде #1") {
      val targets = Array(new TestTarget("bicycle", 1, "ride").asInstanceOf[Target])

      Given("** у пользователя ЕСТЬ право кататься на велосипеде")
      val attributes = Array(new TestAttribute("permissionToRideBicycle", new TestAttributeValue(true)).asInstanceOf[Attribute])

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(1.2) Пользователь #2 хочет покататься на велосипеде #1") {
      val targets = Array(new TestTarget("bicycle", 1, "ride").asInstanceOf[Target])

      Given("** у пользователя НЕТ права кататься на велосипеде")
      val attributes = Array(new TestAttribute("permissionToRideBicycle", new TestAttributeValue(false)).asInstanceOf[Attribute])

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }

    Scenario("(1.3) Пользователь #3 хочет арендовать велосипед #1") {
      val targets = Array(new TestTarget("bicycle", 1, "rent").asInstanceOf[Target])

      Given("** у пользователя ЕСТЬ право арендовать велосипед")
      Given("** пользователю НЕ ИСПОЛНИЛОСЬ 18 лет")
      Given("** пользователь хочет арендовать ТРЁХКОЛЁСНЫЙ велосипед")
      val attributes = Array(
        new TestAttribute("permissionToRentBicycle", new TestAttributeValue(true)).asInstanceOf[Attribute],
        new TestAttribute("personAge", new TestAttributeValue(17)).asInstanceOf[Attribute],
        new TestAttribute("bicycleType", new TestAttributeValue("tricycle")).asInstanceOf[Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(1.4) Пользователь #3 хочет арендовать велосипед #2") {
      val targets = Array(new TestTarget("bicycle", 1, "rent").asInstanceOf[Target])

      Given("** у пользователя ЕСТЬ право арендовать велосипед")
      Given("** пользователю НЕ ИСПОЛНИЛОСЬ 18 лет")
      Given("** пользователь хочет арендовать ДВУХКОЛЁСНЫЙ велосипед")
      val attributes = Array(
        new TestAttribute("permissionToRentBicycle", new TestAttributeValue(true)).asInstanceOf[Attribute],
        new TestAttribute("personAge", new TestAttributeValue(17)).asInstanceOf[Attribute],
        new TestAttribute("bicycleType", new TestAttributeValue("two-wheeled")).asInstanceOf[Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }

    Scenario("(1.5) Пользователь #4 хочет арендовать велосипед #3") {
      val targets = Array(new TestTarget("bicycle", 1, "rent").asInstanceOf[Target])

      Given("** у пользователя ЕСТЬ право арендовать велосипед")
      Given("** пользователю ИСПОЛНИЛОСЬ 18 лет")
      Given("** пользователь хочет арендовать ОДНОКОЛЁСНЫЙ велосипед")
      val attributes = Array(
        new TestAttribute("permissionToRentBicycle", new TestAttributeValue(true)).asInstanceOf[Attribute],
        new TestAttribute("personAge", new TestAttributeValue(18)).asInstanceOf[Attribute],
        new TestAttribute("bicycleType", new TestAttributeValue("unicycle")).asInstanceOf[Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(1.6) Пользователь #5 хочет арендовать велосипед #1") {
      val targets = Array(new TestTarget("bicycle", 1, "rent").asInstanceOf[Target])

      Given("** у пользователя НЕТ ПРАВА арендовать велосипед")
      Given("** пользователю ИСПОЛНИЛОСЬ 18 лет")
      Given("** пользователь хочет арендовать ТРЁХКОЛЁСНЫЙ велосипед")
      val attributes = Array(
        new TestAttribute("permissionToRentBicycle", new TestAttributeValue(false)).asInstanceOf[Attribute],
        new TestAttribute("personAge", new TestAttributeValue(18)).asInstanceOf[Attribute],
        new TestAttribute("bicycleType", new TestAttributeValue("tricycle")).asInstanceOf[Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }
  }

  Feature("(2) Скейты:") {
    Scenario("(2.1) Пользователь #1 хочет покататься на скейте #1") {
      val targets = Array(new TestTarget("skateboard", 1, "ride").asInstanceOf[Target])

      Given("** у пользователя ЕСТЬ право кататься на скейте")
      Given("** пользователь будет кататься на УЛИЦЕ")
      val attributes = Array(
        new TestAttribute("permissionToRideSkateboard", new TestAttributeValue(true)).asInstanceOf[Attribute],
        new TestAttribute("placeType", new TestAttributeValue("street")).asInstanceOf[Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(2.2) Пользователь #2 хочет покататься на скейте #1") {
      val targets = Array(new TestTarget("skateboard", 1, "ride").asInstanceOf[Target])

      Given("** у пользователя НЕТ права кататься на скейте")
      Given("** пользователь будет кататься на УЛИЦЕ")
      val attributes = Array(
        new TestAttribute("permissionToRideSkateboard", new TestAttributeValue(false)).asInstanceOf[Attribute],
        new TestAttribute("placeType", new TestAttributeValue("street")).asInstanceOf[Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }

    Scenario("(2.3) Пользователь #1 хочет покататься на скейте #2") {
      val targets = Array(new TestTarget("skateboard", 1, "ride").asInstanceOf[Target])

      Given("** у пользователя ЕСТЬ право кататься на скейте")
      Given("** пользователь будет кататься в ОФИСЕ")
      val attributes = Array(
        new TestAttribute("permissionToRideSkateboard", new TestAttributeValue(true)).asInstanceOf[Attribute],
        new TestAttribute("placeType", new TestAttributeValue("office")).asInstanceOf[Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }
  }

  Feature("(3) Самокаты:") {
    Scenario("(3.1) Пользователь #1 хочет покататься на самокате #1") {
      val targets = Array(new TestTarget("scooter", 1, "ride").asInstanceOf[Target])

      Given("** у пользователя ЕСТЬ право кататься на скейте")
      Given("** профессия пользователя - КУРЬЕР")
      val attributes = Array(
        new TestAttribute("permissionToRideScooter", new TestAttributeValue(true)).asInstanceOf[Attribute],
        new TestAttribute("profession", new TestAttributeValue("courier")).asInstanceOf[Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(3.2) Пользователь #2 хочет покататься на самокате #1") {
      val targets = Array(new TestTarget("scooter", 1, "ride").asInstanceOf[Target])

      Given("** у пользователя ЕСТЬ право кататься на скейте")
      Given("** профессия пользователя - ПРОГРАММИСТ")
      val attributes = Array(
        new TestAttribute("permissionToRideScooter", new TestAttributeValue(true)).asInstanceOf[Attribute],
        new TestAttribute("profession", new TestAttributeValue("programmer")).asInstanceOf[Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(3.3) Пользователь #3 хочет покататься на самокате #1") {
      val targets = Array(new TestTarget("scooter", 1, "ride").asInstanceOf[Target])

      Given("** у пользователя НЕТ права кататься на скейте")
      Given("** профессия пользователя - КУРЬЕР")
      val attributes = Array(
        new TestAttribute("permissionToRideScooter", new TestAttributeValue(false)).asInstanceOf[Attribute],
        new TestAttribute("profession", new TestAttributeValue("courier")).asInstanceOf[Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(3.4) Пользователь #4 хочет покататься на самокате #1") {
      val targets = Array(new TestTarget("scooter", 1, "ride").asInstanceOf[Target])

      Given("** у пользователя НЕТ права кататься на скейте")
      Given("** профессия пользователя - ПРОГРАММИСТ")
      val attributes = Array(
        new TestAttribute("permissionToRideScooter", new TestAttributeValue(false)).asInstanceOf[Attribute],
        new TestAttribute("profession", new TestAttributeValue("programmer")).asInstanceOf[Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl(policyRetrievalPoint).makeDecision(subject, userId, targets, attributes)

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }
  }
  private def assertDecision(
    decisions: Future[List[TargetedDecision]],
    deny: Boolean = false,
    permit: Boolean = false,
    indeterminate: Boolean = false,
    nonApplicable: Boolean = false
  ): Future[Assertion] = {
    (decisions map { checkless {
      case firstDecision::_ => firstDecision.decision
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
