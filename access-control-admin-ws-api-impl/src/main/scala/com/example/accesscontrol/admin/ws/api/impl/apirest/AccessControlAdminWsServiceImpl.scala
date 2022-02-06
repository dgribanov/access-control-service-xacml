package com.example.accesscontrol.admin.ws.api.impl.apirest

import com.example.accesscontrol.admin.ws.api.impl.domain.{PolicyCollection, PolicySetSerializable}
import com.example.accesscontrol.admin.ws.api.impl.domain.PolicyCollection._
import com.example.accesscontrol.admin.ws.rest.api

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.javadsl.AkkaManagement
import akka.NotUsed
import akka.util.Timeout

import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.EventStreamElement
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.server.LagomApplicationContext
import play.api.Mode

import play.api.libs.json.{JsSuccess, Json}

class AccessControlAdminWsServiceImpl(
  clusterSharding: ClusterSharding,
  persistentEntityRegistry: PersistentEntityRegistry,
  system: ActorSystem,
  context: LagomApplicationContext
)(implicit ec: ExecutionContext) extends api.AccessControlAdminWsService {
  implicit val timeout: Timeout = Timeout(5.seconds)

  AkkaManagement.get(system).start()
  ClusterBootstrap.get(system).start()

  if (context.playContext.environment.mode != Mode.Dev) {
    // Starting the bootstrap process in production
    // ClusterBootstrap.get(system).start()
  }

  /**
   * Looks up the PolicyCollection entity for the given ID.
   */
  private def entityRef(id: String): EntityRef[Command] =
    clusterSharding.entityRefFor(PolicyCollection.typeKey, id)

  override def healthCheck: ServiceCall[NotUsed, String] = ServiceCall {
    _ =>
      Future.successful("Hi from Access Control!")
  }

  override def registerPolicySet(id: String): ServiceCall[api.RegisterPolicySetCommand, api.PolicySetRegisteredResponse] = ServiceCall {
    command =>
      entityRef(id)
        .ask(reply => RegisterPolicySet(id, command.policySet, reply))
        .map { confirmation =>
          confirmationToResponse(confirmation)
        }
  }

  override def policyEventsTopic: Topic[api.PolicyEvent] =
    TopicProducer.taggedStreamWithOffset(Event.Tag) {
      (tag, fromOffset) =>
        persistentEntityRegistry
          .eventStream(tag, fromOffset)
          .mapAsync(4) { case EventStreamElement(id, _, offset) =>
            entityRef(id)
              .ask(reply => ReadPolicyCollection(id, reply))
              .map(policyCollection => convertToMessage(policyCollection) -> offset)
          }
    }

  /**
   * Used for implicit conversion of api.PolicySet to PolicySetSerializable
   */
  implicit def convertRequestPolicySetToDomainPolicySet(
    policySet: api.PolicySet
  ): PolicySetSerializable = Json.fromJson[PolicySetSerializable](Json.toJson(policySet)) match {
    case JsSuccess(policySetSerializable, _) => policySetSerializable
  }

  private def confirmationToResponse(confirmation: Confirmation): api.PolicySetRegisteredResponse =
    confirmation match {
      case Accepted(summary) => convertToResponse(summary)
      case Rejected(reason)  => throw BadRequest(reason)
    }

  private def convertToResponse(summary: Summary): api.PolicySetRegisteredResponse =
    api.PolicySetRegisteredResponse(
      summary.id,
      Json.fromJson[api.PolicySet](Json.toJson(summary.policySet)) match {
        case JsSuccess(policySet, _) => policySet
      }
    )

  private def convertToMessage(policyCollection: PolicyCollection): api.PolicyCollectionRegisteredEvent = {
    val policySets = Json.fromJson[Array[api.PolicySet]](Json.toJson(policyCollection.policySets.values.toArray)) match {
      case JsSuccess(policySets, _) => policySets
    }

    api.PolicyCollectionRegisteredEvent(
      api.PolicyCollection(policyCollection.id.get, policyCollection.version, policySets)
    )
  }
}
