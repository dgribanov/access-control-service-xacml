package com.example.accesscontrol.admin.ws.api.impl.apirest

import scala.concurrent.{ExecutionContext, Future}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import akka.cluster.sharding.typed.scaladsl.EntityRef
import com.example.accesscontrol.admin.ws.api.impl.domain.{PolicyCollection, PolicySetSerializable}
import com.example.accesscontrol.admin.ws.api.impl.domain.PolicyCollection._
import com.example.accesscontrol.admin.ws.rest.api

import scala.concurrent.duration._
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.example.accesscontrol.admin.ws.rest.api.PolicySet
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.EventStreamElement

class AccessControlAdminWsServiceImpl(
  clusterSharding: ClusterSharding,
  persistentEntityRegistry: PersistentEntityRegistry
)(implicit ec: ExecutionContext) extends api.AccessControlAdminWsService {
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

  override def registerPolicySet: ServiceCall[api.RegisterPolicySetCommand, api.PolicySetRegisteredResponse] = ServiceCall {
    command =>
      entityRef(PolicyCollection.version)
        .ask(reply => RegisterPolicySet(command.policySet.asInstanceOf[PolicySetSerializable], reply))
        .map { confirmation =>
          confirmationToResponse(confirmation)
        }
  }

  override def sendPolicySetRegisteredEventMessage: Topic[api.PolicySetRegisteredEvent] =
    TopicProducer.taggedStreamWithOffset(Event.Tag) {
      (tag, fromOffset) =>
        persistentEntityRegistry
          .eventStream(tag, fromOffset)
          .mapAsync(4) { case EventStreamElement(id, _, offset) =>
            entityRef(id)
              .ask(reply => ReadPolicyCollection(reply))
              .map(policyCollection => convertToMessage(policyCollection) -> offset)
          }
    }

  private def confirmationToResponse(confirmation: Confirmation): api.PolicySetRegisteredResponse =
    confirmation match {
      case Accepted(summary) => convertToResponse(summary)
      case Rejected(reason)  => throw BadRequest(reason)
    }

  private def convertToResponse(summary: Summary): api.PolicySetRegisteredResponse =
    api.PolicySetRegisteredResponse(
      PolicyCollection.version,
      summary.policySet.asInstanceOf[PolicySet]
    )

  private def convertToMessage(policyCollection: PolicyCollection): api.PolicySetRegisteredEvent =
    api.PolicySetRegisteredEvent(policyCollection.asInstanceOf[api.PolicyCollection])
}
