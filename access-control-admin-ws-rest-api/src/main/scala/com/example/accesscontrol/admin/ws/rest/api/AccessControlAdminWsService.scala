package com.example.accesscontrol.admin.ws.rest.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.transport.Method
import play.api.libs.json.{Format, Json}

trait AccessControlAdminWsService extends Service {
  /**
   * Example: curl http://localhost:9000/healthcheck/access-control-admin
   */
  def healthcheck: ServiceCall[NotUsed, String]

  /**
   * Example: curl -H "Content-Type: application/json" -X POST
   *
   * http://localhost:9000/api/access-control-admin/register-policy-set
   */
  def registerPolicySet: ServiceCall[RegisterPolicySetCommand, PolicySetRegisteredResponse]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("access-control-admin")
      .withCalls(
        restCall(Method.GET, "/healthcheck/access-control-admin", healthcheck),
        restCall(Method.POST, "api/access-control-admin/register-policy-set", registerPolicySet),
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

final case class PolicySetRegisteredResponse(version: String, policySet: PolicySet)

object PolicySetRegisteredResponse {
  implicit val format: Format[PolicySetRegisteredResponse] = Json.format
}
