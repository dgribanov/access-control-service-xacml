package com.example.accesscontrol.api.impl.apirest

import com.example.accesscontrol.api.impl.{
  AccessControlApiApplication,
  BaseAcceptanceSpec,
  TestAccessControlModule
}
import com.example.accesscontrol.rest.api.{
  AccessControlService,
  AccessControlRequest,
  AccessControlSuccessResponse,
  AccessControlError,
  Target
}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest

class AccessControlRestApiServiceAcceptanceSpec extends BaseAcceptanceSpec {
  /**
   * Run server once for all tests in spec and stop it after tests are completed.
   * Look at another way of up and down server for each test in Lagom documentation:
   * https://www.lagomframework.com/documentation/1.6.x/scala/TestingServices.html#How-to-test-one-service
   */
  lazy private implicit val module: TestAccessControlModule = new TestAccessControlModule
  lazy private val server = ServiceTest.startServer(ServiceTest.defaultSetup) { ctx =>
    new AccessControlApiApplication(ctx) with LocalServiceLocator
  }

  lazy val client: AccessControlService = server.serviceClient.implement[AccessControlService]

  override protected def beforeAll(): Unit = server
  override protected def afterAll(): Unit = server.stop()

  info("Как сервис, использующий AccessControl, я хочу убедиться, что он работает и отдаёт корректный ответ")

  Feature("Метод `проверки пульса`") {
    Scenario("Хочу проверить пульс у сервиса AccessControl") {

      When(">>> делаю запрос на ручку `проверки пульса`")
      val response = client.healthcheck.invoke()

      Then("<<< убеждаюсь, что пульс прощупывается, пациент скорее жив...")
      response map { answer =>
        assert(answer == "Hi from Access Control!")
      }
    }
  }

  Feature("Метод проверки доступа пользователя") {
    Scenario("Хочу проверить доступ пользователя (субъекта доступа) к действию над определённым объектом") {

      Given("** проверяю доступ пользователя типа User с ID `user1`")
      Given("** к действию кататься (`ride`) в отношении объекта типа велосипед (`bicycle`) с ID 1")
      val request = AccessControlRequest(
        Array(Target(objectType = "bicycle", objectId = 1, action = "ride")),
        Array(/*Attribute(name = "permissionToRideBicycle", value = AttributeValueBool(value = true))*/)
      )

      When(">>> делаю запрос на ручку проверки доступа")
      val response = client.check("user", "user1").invoke(request)

      Then("<<< убеждаюсь, проверка доступа работает, ответ корректный")
      response map { answer =>
        assert(answer match {
          case _: AccessControlSuccessResponse => true
          case _: AccessControlError           => false
        })
      }
    }
  }
}
