package com.example.accesscontrol.api.impl.data.mapping

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.util.Timeout
import play.api.libs.json.{JsError, JsSuccess, Json}

import com.example.accesscontrol.api.impl.domain.{
  Policy,
  PolicyCollection,
  PolicyRepository,
  PolicyRetrievalPoint,
  PolicySet,
  TargetType,
}
import com.example.accesscontrol.api.impl.data.mapping.PolicyCollectionManager.{
  FetchPolicy,
  FetchPolicyCollection,
  FetchPolicySet,
  RegistryPolicyCollection,
}

final class PolicyRetrievalPointImpl @Inject() (policyRepository: PolicyRepository) extends PolicyRetrievalPoint {
  private val system = ActorSystem("policy-retrieval-point-system")
  private val policyCollectionManagerActorRef = system.actorOf(PolicyCollectionManager.props, "policy-collection-manager")
  buildPolicyCollection()

  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future
  implicit val timeout: Timeout = Timeout(1.nanosecond) // don`t move! it`s implicit Timeout for Akka ask (?) operator

  def fetchPolicyCollection(): Future[Option[PolicyCollection]] = {
    import akka.pattern.ask

    (policyCollectionManagerActorRef ? FetchPolicyCollection()).mapTo[Option[PolicyCollection]]
  }

  def fetchPolicySet(target: TargetType): Future[Option[PolicySet]] = {
    import akka.pattern.ask

    (policyCollectionManagerActorRef ? FetchPolicySet(target)).mapTo[Option[PolicySet]]
  }

  def fetchPolicy(policySetTarget: TargetType, policyTarget: TargetType): Future[Option[Policy]] = {
    import akka.pattern.ask

    (policyCollectionManagerActorRef ? FetchPolicy(policySetTarget, policyTarget)).mapTo[Option[Policy]]
  }

  def buildPolicyCollection(): Unit = {
      Json.fromJson[PolicyCollectionSerializable](policyRepository.fetchPolicyCollection) match {
        case JsSuccess(policyCollection, _) => policyCollectionManagerActorRef ! RegistryPolicyCollection(policyCollection)
        case JsError.Message(errMsg)        => PolicyCollectionParsingError(errMsg)
      }
  }
}
