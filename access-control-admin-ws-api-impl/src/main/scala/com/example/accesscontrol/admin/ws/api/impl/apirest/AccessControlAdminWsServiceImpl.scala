package com.example.accesscontrol.admin.ws.api.impl.apirest

import scala.concurrent.{ExecutionContext, Future}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import akka.cluster.sharding.typed.scaladsl.EntityRef
import com.example.accesscontrol.admin.ws.api.impl.domain.{PolicyCollection, PolicySetSerializable}
import com.example.accesscontrol.admin.ws.api.impl.domain.PolicyCollection._
import com.example.accesscontrol.admin.ws.rest.api.{AccessControlAdminWsService, RegisterPolicySetCommand, PolicySetRegisteredResponse}
import scala.concurrent.duration._
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.example.accesscontrol.admin.ws.rest.api.PolicySet

class AccessControlAdminWsServiceImpl(
  clusterSharding: ClusterSharding,
  persistentEntityRegistry: PersistentEntityRegistry
)(implicit ec: ExecutionContext) extends AccessControlAdminWsService {
  implicit val timeout: Timeout = Timeout(5.seconds)

  /**
   * Looks up the PolicyCollection entity for the given ID.
   */
  private def entityRef(id: String): EntityRef[Command] =
    clusterSharding.entityRefFor(PolicyCollection.typeKey, id)

  override def healthcheck: ServiceCall[NotUsed, String] = ServiceCall {
    _ =>
      Future.successful("Hi from Access Control!")
  }

  override def registerPolicySet: ServiceCall[RegisterPolicySetCommand, PolicySetRegisteredResponse] = ServiceCall {
    command =>
      entityRef(PolicyCollection.version)
        .ask(reply => RegisterPolicySet(command.policySet.asInstanceOf[PolicySetSerializable], reply))
        .map { confirmation =>
          confirmationToResult(confirmation)
        }
  }

  private def confirmationToResult(confirmation: Confirmation): PolicySetRegisteredResponse =
    confirmation match {
      case Accepted(summary) => convertToResponse(summary)
      case Rejected(reason)  => throw BadRequest(reason)
    }

  private def convertToResponse(summary: Summary): PolicySetRegisteredResponse = {
    PolicySetRegisteredResponse(
      PolicyCollection.version,
      summary.policySet.asInstanceOf[PolicySet]
    )
  }
}
