package com.example.accesscontrol.rest.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}

trait AccessControlService extends Service {

  /**
   * Example: curl http://localhost:9000/api/access-control/hi
   */
  def hello: ServiceCall[NotUsed, String]

  /**
   * Example: curl -H "Content-Type: application/json" -X POST
   * -d '{"targets": [{"objectType": "bicycle", "action": "ride"}], "attributes": [{"name": "userId", "value": {"_type": "int", "value": 1}}, {"name": "permissionToRideBicycle", "value": {"_type": "bool", "value": true}}]}'
   * http://localhost:9000/api/access-control/check/bla/blo
   */
  def check(subject: String, id: String): ServiceCall[AccessControlRequest, AccessControlResponse]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("access-control")
      .withCalls(
        pathCall("/api/access-control/hi", hello),
        pathCall("/api/access-control/check/:subject/:id", check _),
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}
