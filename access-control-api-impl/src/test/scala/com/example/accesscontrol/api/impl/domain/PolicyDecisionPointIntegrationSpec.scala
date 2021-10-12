package com.example.accesscontrol.api.impl.domain

import com.example.accesscontrol.api.impl.infrastructure.TestPolicyRepositoryImpl
import com.example.accesscontrol.api.impl.BaseIntegrationSpec
import org.scalatest.Assertion

import scala.concurrent.Future

class Target (val objectType: String, val objectId: Int, val action: String)
class Attribute (val name: String, val value: AttributeValue)
class AttributeValue (val value: Any)

class PolicyDecisionPointIntegrationSpec extends BaseIntegrationSpec {

  implicit val policyRepository: PolicyRepository = new TestPolicyRepositoryImpl
  implicit val policyCollectionFetch: PolicyDecisionPointImpl.PolicyCollectionFetch =
    PolicyRetrievalPoint().buildPolicyCollection

  info("Как сервис, управляющий продажей и арендой велосипедов, самокатов и скейтов, я хочу проверить доступ пользователя к основным действиям с моими товарами.")

  Feature("(0) Танки:") {
    Scenario("(0.1) Пользователь #1 хочет покататься на танке #1") {
      val targets = Array(new Target("tank", 1, "ride").asInstanceOf[PolicyDecisionPointImpl.Target])

      Given("** у пользователя ЕСТЬ право кататься на танке (он танкист)")
      val attributes = Array(new Attribute("permissionToRideBicycle", new AttributeValue(true)).asInstanceOf[PolicyDecisionPointImpl.Attribute])

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl.makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]]) }

      Then("<<< убеждаемся, что проверка доступа НЕ ПРИМЕНИМА для объекта `танк`, МЫ МИРНЫЕ ЛЮДИ, У НАС НЕТ ТАНКОВ! (пока нет)")
      assertDecision(decisions, nonApplicable = true)
    }
  }

  Feature("(1) Велосипеды:") {
    Scenario("(1.1) Пользователь #1 хочет покататься на велосипеде #1") {
      val targets = Array(new Target("bicycle", 1, "ride").asInstanceOf[PolicyDecisionPointImpl.Target])

      Given("** у пользователя ЕСТЬ право кататься на велосипеде")
      val attributes = Array(new Attribute("permissionToRideBicycle", new AttributeValue(true)).asInstanceOf[PolicyDecisionPointImpl.Attribute])

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl.makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]])}

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(1.2) Пользователь #2 хочет покататься на велосипеде #1") {
      val targets = Array(new Target("bicycle", 1, "ride").asInstanceOf[PolicyDecisionPointImpl.Target])

      Given("** у пользователя НЕТ права кататься на велосипеде")
      val attributes = Array(new Attribute("permissionToRideBicycle", new AttributeValue(false)).asInstanceOf[PolicyDecisionPointImpl.Attribute])

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl.makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]])}

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }

    Scenario("(1.3) Пользователь #3 хочет арендовать велосипед #1") {
      val targets = Array(new Target("bicycle", 1, "rent").asInstanceOf[PolicyDecisionPointImpl.Target])

      Given("** у пользователя ЕСТЬ право арендовать велосипед")
      Given("** пользователю НЕ ИСПОЛНИЛОСЬ 18 лет")
      Given("** пользователь хочет арендовать ТРЁХКОЛЁСНЫЙ велосипед")
      val attributes = Array(
        new Attribute("permissionToRentBicycle", new AttributeValue(true)).asInstanceOf[PolicyDecisionPointImpl.Attribute],
        new Attribute("personAge", new AttributeValue(17)).asInstanceOf[PolicyDecisionPointImpl.Attribute],
        new Attribute("bicycleType", new AttributeValue("tricycle")).asInstanceOf[PolicyDecisionPointImpl.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl.makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]])}

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(1.4) Пользователь #3 хочет арендовать велосипед #2") {
      val targets = Array(new Target("bicycle", 1, "rent").asInstanceOf[PolicyDecisionPointImpl.Target])

      Given("** у пользователя ЕСТЬ право арендовать велосипед")
      Given("** пользователю НЕ ИСПОЛНИЛОСЬ 18 лет")
      Given("** пользователь хочет арендовать ДВУХКОЛЁСНЫЙ велосипед")
      val attributes = Array(
        new Attribute("permissionToRentBicycle", new AttributeValue(true)).asInstanceOf[PolicyDecisionPointImpl.Attribute],
        new Attribute("personAge", new AttributeValue(17)).asInstanceOf[PolicyDecisionPointImpl.Attribute],
        new Attribute("bicycleType", new AttributeValue("two-wheeled")).asInstanceOf[PolicyDecisionPointImpl.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl.makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]])}

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }

    Scenario("(1.5) Пользователь #4 хочет арендовать велосипед #3") {
      val targets = Array(new Target("bicycle", 1, "rent").asInstanceOf[PolicyDecisionPointImpl.Target])

      Given("** у пользователя ЕСТЬ право арендовать велосипед")
      Given("** пользователю ИСПОЛНИЛОСЬ 18 лет")
      Given("** пользователь хочет арендовать ОДНОКОЛЁСНЫЙ велосипед")
      val attributes = Array(
        new Attribute("permissionToRentBicycle", new AttributeValue(true)).asInstanceOf[PolicyDecisionPointImpl.Attribute],
        new Attribute("personAge", new AttributeValue(18)).asInstanceOf[PolicyDecisionPointImpl.Attribute],
        new Attribute("bicycleType", new AttributeValue("unicycle")).asInstanceOf[PolicyDecisionPointImpl.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl.makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]])}

      Then("<<< убеждаемся, что пользователь ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, permit = true)
    }

    Scenario("(1.6) Пользователь #5 хочет арендовать велосипед #1") {
      val targets = Array(new Target("bicycle", 1, "rent").asInstanceOf[PolicyDecisionPointImpl.Target])

      Given("** у пользователя НЕТ ПРАВА арендовать велосипед")
      Given("** пользователю ИСПОЛНИЛОСЬ 18 лет")
      Given("** пользователь хочет арендовать ТРЁХКОЛЁСНЫЙ велосипед")
      val attributes = Array(
        new Attribute("permissionToRentBicycle", new AttributeValue(false)).asInstanceOf[PolicyDecisionPointImpl.Attribute],
        new Attribute("personAge", new AttributeValue(18)).asInstanceOf[PolicyDecisionPointImpl.Attribute],
        new Attribute("bicycleType", new AttributeValue("tricycle")).asInstanceOf[PolicyDecisionPointImpl.Attribute],
      )

      When(">>> запрашиваем проверку доступа")
      val decisions = PolicyDecisionPointImpl.makeDecision(targets, attributes)

      Then("<<< видим, что проверка доступа удалась, ошибок нет")
      decisions map { decision => assert(decision.isInstanceOf[Right[_, Array[_]]])}

      Then("<<< убеждаемся, что пользователь НЕ ПОЛУЧАЕТ необходимый доступ")
      assertDecision(decisions, deny = true)
    }
  }

  private def assertDecision(
                              decisions: Future[Either[DomainException, Array[TargetedDecision]]],
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
      case _: Decision.Deny          => deny
      case _: Decision.Permit        => permit
      case _: Decision.Indeterminate => indeterminate
      case _: Decision.NonApplicable => nonApplicable
    })}
  }

  /**
   * Funny hack for suppression compiler standard warning 'match is not exhaustive'
   * when Scala `@unchecked` annotation not applicable
   * @see https://stackoverflow.com/questions/10507419/scala-where-to-put-the-unchecked-annotation-in-a-foreach
   */
  private def checkless[A,B](pf: PartialFunction[A,B]): A => B = pf: A => B
}
