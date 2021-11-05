package com.example.accesscontrol.api.impl.data.mapping

import com.example.accesscontrol.api.impl.domain.{
  PolicyCollection,
  PolicyRepository,
  PolicyRetrievalPoint
}
import play.api.libs.json.{JsError, JsSuccess, Json}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.util.Timeout

import scala.concurrent.duration._
import com.example.accesscontrol.api.impl.data.mapping.PolicyCollectionKeeper.{FetchPolicyCollection, RegistryPolicyCollection}

final class PolicyRetrievalPointImpl @Inject() (policyRepository: PolicyRepository) extends PolicyRetrievalPoint {
  private val system = ActorSystem("policy-retrieval-point-system")
  private val policyCollectionKeeperActorRef = system.actorOf(PolicyCollectionKeeper.props, "policy-collection-keeper")
  buildPolicyCollection()

  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future
  implicit val timeout: Timeout = Timeout(5.seconds) // don`t move! it`s implicit Timeout for Akka ask (?) operator

  def fetchPolicyCollection(): Future[Option[PolicyCollection]] = {
    import akka.pattern.ask

    (policyCollectionKeeperActorRef ? FetchPolicyCollection()).mapTo[Option[PolicyCollection]]
  }

  def buildPolicyCollection(): Unit = {
      Json.fromJson[PolicyCollectionSerializable](policyRepository.fetchPolicyCollection) match {
        case JsSuccess(policyCollection, _) => policyCollectionKeeperActorRef ! RegistryPolicyCollection(policyCollection)
        case JsError.Message(errMsg)        => PolicyCollectionParsingError(errMsg)
      }
  }
}
