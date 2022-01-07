package com.example.accesscontrol.api.impl.data.mapping

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

import akka.actor.typed.{ActorSystem, Scheduler}
import akka.actor.typed.scaladsl.AskPattern._
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
import com.example.accesscontrol.api.impl.data.mapping.PolicyDataProvider.{
  FetchPolicy,
  FetchPolicyCollection,
  FetchPolicySet,
  RegistryPolicyCollection,
}

final class PolicyRetrievalPointImpl @Inject() (policyRepository: PolicyRepository) extends PolicyRetrievalPoint {
  private val actorSystem: ActorSystem[PolicyDataProvider.Message] = ActorSystem(PolicyDataProvider(), "policy-data-system")
  buildPolicyCollection()

  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future
  implicit val timeout: Timeout = Timeout(1.nanosecond) // don`t move! it`s implicit Timeout for Akka ask (?) operator
  implicit val scheduler: Scheduler = actorSystem.scheduler // don`t move! it`s implicit scheduler for Akka ask (?) operator

  def fetchPolicyCollection(subject: String): Future[Option[PolicyCollection]] =
    actorSystem ? (ref => FetchPolicyCollection(subject, ref))

  def fetchPolicySet(subject: String, target: TargetType): Future[Option[PolicySet]] =
    actorSystem ? (ref => FetchPolicySet(subject, target, ref))

  def fetchPolicy(subject: String, policySetTarget: TargetType, policyTarget: TargetType): Future[Option[Policy]] =
    actorSystem ? (ref => FetchPolicy(subject, policySetTarget, policyTarget, ref))

  def buildPolicyCollection(): Unit =
    Json.fromJson[Array[PolicyCollectionSerializable]](policyRepository.fetchPolicyCollections) match {
      case JsSuccess(policyCollections, _) => policyCollections foreach {policyCollection => actorSystem ! RegistryPolicyCollection(policyCollection)}
      case JsError.Message(errMsg)         => PolicyCollectionParsingError(errMsg)
    }
}
