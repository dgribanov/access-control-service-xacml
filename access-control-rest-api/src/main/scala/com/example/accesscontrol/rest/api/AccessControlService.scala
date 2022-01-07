package com.example.accesscontrol.rest.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.transport.Method

trait AccessControlService extends Service {

  /**
   * Example: curl http://localhost:9000/healthcheck/access-control
   */
  def healthcheck: ServiceCall[NotUsed, String]

  /**
   * Example: curl -H "Content-Type: application/json" -X POST
   * -d '{"targets": [{"objectType": "bicycle", "objectId": 1, "action": "ride"}], "attributes": [{"name": "userId", "value": {"_type": "int", "value": 1}}, {"name": "permissionToRideBicycle", "value": {"_type": "bool", "value": true}}]}'
   * http://localhost:9000/api/access-control/check/user/bob
   */
  def check(subject: String, id: String): ServiceCall[AccessControlRequest, AccessControlResponse]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("access-control")
      .withCalls(
        restCall(Method.GET, "/healthcheck/access-control", healthcheck),
        restCall(Method.POST, "/api/access-control/check/:subject/:id", check _),
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}
