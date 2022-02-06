package com.example.accesscontrol.admin.ws.rest.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.transport.Method
import play.api.libs.json.{Format, Json}

object AccessControlAdminWsService {
  val TOPIC_NAME = "access-control-policies"
}
trait AccessControlAdminWsService extends Service {
  /**
   * Example: curl http://localhost:9000/admin/health-check
   */
  def healthCheck: ServiceCall[NotUsed, String]

  /**
   * Example: curl -H "Content-Type: application/json" -X POST
   *
   * http://localhost:9000/admin/api/policy-collection/:id/policy-set
   */
  def registerPolicySet(id: String): ServiceCall[RegisterPolicySetCommand, PolicySetRegisteredResponse]

  def policyEventsTopic: Topic[PolicyEvent]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("access-control-admin")
      .withCalls(
        restCall(Method.GET, "/admin/health-check", healthCheck),
        restCall(Method.POST, "/admin/api/policy-collection/:id/policy-set", registerPolicySet _),
      )
      .withTopics(
        topic(AccessControlAdminWsService.TOPIC_NAME, policyEventsTopic)
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

final case class PolicySetRegisteredResponse(id: String, policySet: PolicySet)

object PolicySetRegisteredResponse {
  implicit val format: Format[PolicySetRegisteredResponse] = Json.format
}

abstract class PolicyEvent
final case class PolicyCollectionRegisteredEvent(policyCollection: PolicyCollection) extends PolicyEvent

object PolicyCollectionRegisteredEvent {
  implicit val format: Format[PolicyCollectionRegisteredEvent] = Json.format
}

object PolicyEvent {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[PolicyEvent] = Format[PolicyEvent] (
    Reads { js =>
      // use the _type field to determine how to deserialize
      val valueType = (JsPath \ "_type").read[String].reads(js)
      valueType.fold(
        _ => JsError("type undefined or incorrect"),
        {
          case "PolicyCollectionRegisteredEvent" => JsPath.read[PolicyCollectionRegisteredEvent].reads(js)
        }
      )
    },
    Writes {
      case event: PolicyCollectionRegisteredEvent =>
        JsObject(
          Seq(
            "_type"            -> JsString("PolicyCollectionRegisteredEvent"),
            "policyCollection" -> PolicyCollection.format.writes(event.policyCollection)
          )
        )
    }
  )
}
