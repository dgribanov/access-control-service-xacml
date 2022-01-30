package com.example.accesscontrol.api.impl.data.mapping

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import com.example.accesscontrol.api.impl.domain.{Policy, PolicyCollection, PolicySet, TargetType}

object PolicyDataProvider {
  sealed trait Message

  sealed trait Command extends Message
  final case class RegistryPolicyCollection(policyCollection: PolicyCollection) extends Command
  final case class RegistryPolicySet(policySet: PolicySet) extends Command
  final case class RegistryPolicy(policy: Policy) extends Command

  sealed trait Query extends Message
  final case class FetchPolicyCollection(subject: String, replyTo: ActorRef[Option[PolicyCollection]]) extends Command
  final case class FetchPolicySet(subject: String, target: TargetType, replyTo: ActorRef[Option[PolicySet]]) extends Command
  final case class FetchPolicy(subject: String, policySetTarget: TargetType, policyTarget: TargetType, replyTo: ActorRef[Option[Policy]]) extends Command

  private val policyCollectionMap = Map.empty[String, ActorRef[Message]]

  def apply(): Behavior[Message] =
    onMessage(policyCollectionMap)

  private def onMessage(policyCollectionMap: Map[String, ActorRef[Message]]): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      (message: @unchecked) match {
        case command @ RegistryPolicyCollection(policyCollection) =>
          val newPolicyCollectionMap = if (policyCollectionMap.contains(policyCollection.id)) {
            policyCollectionMap.get(policyCollection.id) match {
              case Some(sendTo) => sendTo ! command
            }
            policyCollectionMap
          } else policyCollectionMap + (policyCollection.id -> spawnChild(context, policyCollection))
          onMessage(newPolicyCollectionMap)
        case command @ FetchPolicyCollection(subject, replyTo) =>
          policyCollectionMap.get(subject) match {
            case Some(policyCollection) => policyCollection ! command
            case None                   => replyTo ! None
          }
          Behaviors.same
        case command @ FetchPolicySet(subject, _, replyTo) =>
          policyCollectionMap.get(subject) match {
            case Some(policyCollection) => policyCollection ! command
            case None                   => replyTo ! None
          }
          Behaviors.same
        case command @ FetchPolicy(subject, _, _, replyTo) =>
          policyCollectionMap.get(subject) match {
            case Some(policyCollection) => policyCollection ! command
            case None                   => replyTo ! None
          }
          Behaviors.same
      }
    }

  private def spawnChild(context: ActorContext[Message], policyCollection: PolicyCollection): ActorRef[Message] = {
    val childActorRef = context.spawn(PolicyCollectionProvider(), s"policy-collection-${policyCollection.id}")
    childActorRef ! RegistryPolicyCollection(policyCollection)
    childActorRef
  }
}

object PolicyCollectionProvider {
  import PolicyDataProvider._

  private val policyCollection: Option[PolicyCollection] = None
  private val policySetMap = Map.empty[String, ActorRef[Message]]

  def apply(): Behavior[Message] =
    onMessage(policyCollection, policySetMap)

  private def onMessage(policyCollection: Option[PolicyCollection], policySetMap: Map[String, ActorRef[Message]]): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      (message: @unchecked) match {
        case RegistryPolicyCollection(policyCollection) =>
          context.log.info("Registry policy collection: {}", policyCollection)
          val newPolicySetMap = policyCollection.policySets.map(
            policySet => policySet.target.value -> spawnChild(context, policySet)
          ).toMap
          onMessage(Some(policyCollection), newPolicySetMap)
        case FetchPolicyCollection(_, replyTo) =>
          replyTo ! policyCollection
          Behaviors.same
        case command @ FetchPolicySet(_, target, replyTo) =>
          policySetMap.get(target.value) match {
            case Some(policySet) => policySet ! command
            case None            => replyTo ! None
          }
          Behaviors.same
        case command @ FetchPolicy(_, target, _, replyTo) =>
          policySetMap.get(target.value) match {
            case Some(policySet) => policySet ! command
            case None            => replyTo ! None
          }
          Behaviors.same
      }
  }

  private def spawnChild(context: ActorContext[Message], policySet: PolicySet): ActorRef[Message] = {
    val childActorRef = context.spawn(PolicySetProvider(), s"policy-set-${policySet.target.value}")
    childActorRef ! RegistryPolicySet(policySet)
    childActorRef
  }
}

object PolicySetProvider {
  import PolicyDataProvider._

  private val policySet: Option[PolicySet] = None
  private val policyMap = Map.empty[String, ActorRef[Message]]

  def apply(): Behavior[Message] =
    onMessage(policySet, policyMap)

  private def onMessage(policySet: Option[PolicySet], policyMap: Map[String, ActorRef[Message]]): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      (message: @unchecked) match {
        case RegistryPolicySet(policySet) =>
          context.log.info("Registry policy set: {}", policySet)
          val newPolicyMap = policySet.policies.map(
            policy => policy.target.value -> spawnChild(context, policy)
          ).toMap
          onMessage(Some(policySet), newPolicyMap)
        case FetchPolicySet(_, target, replyTo) =>
          policySet match {
            case Some(ps) if ps.target.value == target.value => replyTo ! Some(ps)
          }
          Behaviors.same
        case command @ FetchPolicy(_, _, target, _) =>
          policyMap.get(target.value) match {
            case Some(policy) => policy ! command
            case _            =>
          }
          Behaviors.same
      }
    }

  private def spawnChild(context: ActorContext[Message], policy: Policy): ActorRef[Message] = {
    val childActorRef = context.spawn(PolicyProvider(), s"policy-${policy.target.value}")
    childActorRef ! RegistryPolicy(policy)
    childActorRef
  }
}

object PolicyProvider {
  import PolicyDataProvider._

  private val policy: Option[Policy] = None

  def apply(): Behavior[Message] =
    onMessage(policy)

  private def onMessage(policy: Option[Policy]): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      (message: @unchecked) match {
        case RegistryPolicy(policy) =>
          context.log.info("Registry policy: {}", policy)
          onMessage(Some(policy))
        case FetchPolicy(_, _, target, replyTo) =>
          policy match {
            case Some(p) if p.target.value == target.value => replyTo ! Some(p)
          }
          Behaviors.same
      }
    }
}
