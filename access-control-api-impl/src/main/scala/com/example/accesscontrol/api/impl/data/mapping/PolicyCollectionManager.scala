package com.example.accesscontrol.api.impl.data.mapping

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import com.example.accesscontrol.api.impl.domain.{Policy, PolicyCollection, PolicySet, TargetType}

object PolicyCollectionManager {
  sealed trait Message

  sealed trait Command extends Message
  final case class RegistryPolicyCollection(policyCollection: PolicyCollection) extends Command
  final case class RegistryPolicySet(policySet: PolicySet) extends Command
  final case class RegistryPolicy(policy: Policy) extends Command

  sealed trait Query extends Message
  final case class FetchPolicyCollection(replyTo: ActorRef[Option[PolicyCollection]]) extends Command
  final case class FetchPolicySet(target: TargetType, replyTo: ActorRef[Option[PolicySet]]) extends Command
  final case class FetchPolicy(policySetTarget: TargetType, policyTarget: TargetType, replyTo: ActorRef[Option[Policy]]) extends Command

  private val policyCollection: Option[PolicyCollection] = None
  private val policySetMap = Map.empty[String, ActorRef[Message]]

  def apply(): Behavior[Message] = {
    onMessage(policyCollection, policySetMap)
  }

  private def onMessage(policyCollection: Option[PolicyCollection], policySetMap: Map[String, ActorRef[Message]]): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      (message: @unchecked) match {
        case RegistryPolicyCollection(policyCollection) =>
          context.log.info("Registry policy collection: {}", policyCollection)
          val policySetMap = policyCollection.policySets.map(
            policySet => policySet.target.value -> spawnChild(context, policySet)
          ).toMap
          onMessage(Some(policyCollection), policySetMap)
        case FetchPolicyCollection(replyTo) =>
          replyTo ! policyCollection
          Behaviors.same
        case command @ FetchPolicySet(target, replyTo) =>
          policySetMap.get(target.value) match {
            case Some(policySet) => policySet ! command
            case None            => replyTo ! None
          }
          Behaviors.same
        case command @ FetchPolicy(target, _, replyTo) =>
          policySetMap.get(target.value) match {
            case Some(policySet) => policySet ! command
            case None            => replyTo ! None
          }
          Behaviors.same
      }
  }

  private def spawnChild(context: ActorContext[Message], policySet: PolicySet): ActorRef[Message] = {
    val childActorRef = context.spawn(PolicySetManager(), s"policy-set-${policySet.target.value}")
    childActorRef ! RegistryPolicySet(policySet)
    childActorRef
  }
}

object PolicySetManager {
  import PolicyCollectionManager._

  private val policySet: Option[PolicySet] = None
  private val policyMap = Map.empty[String, ActorRef[Message]]

  def apply(): Behavior[Message] = {
    onMessage(policySet, policyMap)
  }

  private def onMessage(policySet: Option[PolicySet], policyMap: Map[String, ActorRef[Message]]): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      (message: @unchecked) match {
        case RegistryPolicySet(policySet) =>
          context.log.info("Registry policy set: {}", policySet)
          val policyMap = policySet.policies.map(
            policy => policy.target.value -> spawnChild(context, policy)
          ).toMap
          onMessage(Some(policySet), policyMap)
        case FetchPolicySet(target, replyTo) =>
          policySet match {
            case Some(ps) if ps.target.value == target.value => replyTo ! Some(ps)
          }
          Behaviors.same
        case command @ FetchPolicy(_, target, _) =>
          policyMap.get(target.value) match {
            case Some(policy) => policy ! command
            case _            =>
          }
          Behaviors.same
      }
    }

  private def spawnChild(context: ActorContext[Message], policy: Policy): ActorRef[Message] = {
    val childActorRef = context.spawn(PolicyManager(), s"policy-${policy.target.value}")
    childActorRef ! RegistryPolicy(policy)
    childActorRef
  }
}

object PolicyManager {
  import PolicyCollectionManager._

  private val policy: Option[Policy] = None

  def apply(): Behavior[Message] = {
    onMessage(policy)
  }

  private def onMessage(policy: Option[Policy]): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      (message: @unchecked) match {
        case RegistryPolicy(policy) =>
          context.log.info("Registry policy: {}", policy)
          onMessage(Some(policy))
        case FetchPolicy(_, target, replyTo) =>
          policy match {
            case Some(p) if p.target.value == target.value => replyTo ! Some(p)
          }
          Behaviors.same
      }
    }
}
