package com.example.accesscontrol.api.impl.data.mapping

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.typed.scaladsl.Behaviors
import akka.event.Logging

import com.example.accesscontrol.api.impl.domain.{PolicyCollection, PolicySet, Policy, TargetType}

object PolicyCollectionManager {
  def props: Props = Props[PolicyCollectionManager]

  sealed trait Command
  case class RegistryPolicyCollection(policyCollection: PolicyCollection) extends Command
  case class RegistryPolicySet(policySet: PolicySet) extends Command
  case class RegistryPolicy(policy: Policy) extends Command

  case class FetchPolicyCollection() extends Command
  case class FetchPolicySet(target: TargetType) extends Command
  case class FetchPolicy(policySetTarget: TargetType, policyTarget: TargetType) extends Command
}

class PolicyCollectionManager extends Actor {
  import PolicyCollectionManager._
  private val log = Logging.getLogger(context.system, this)

  private val policyCollection: Option[PolicyCollection] = None
  private val policySetMap = Map.empty[String, ActorRef]

  override def receive: Receive = onMessage(policyCollection, policySetMap)

  private def onMessage(policyCollection: Option[PolicyCollection], policySetMap: Map[String, ActorRef]): Receive = {
    case RegistryPolicyCollection(pc) =>
      log.info("Registry policy collection: {}", pc)
      val psMap = pc.policySets.map(
        policySet => policySet.target.value -> spawnChild(policySet)
      ).toMap
      context.become(onMessage(Some(pc), psMap))
    case _: FetchPolicyCollection =>
      sender ! policyCollection
    case command @ FetchPolicySet(target) =>
      policySetMap.get(target.value) match {
        case Some(policySet) => policySet forward command
        case None            => sender ! None
      }
    case command @ FetchPolicy(target, _) =>
      policySetMap.get(target.value) match {
        case Some(policySet) => policySet forward command
        case None            => sender ! None
      }
  }

  private def spawnChild(policySet: PolicySet): ActorRef = {
    val childActorRef = context.actorOf(Props[PolicySetManager], s"policy-set-${policySet.target.value}")
    childActorRef ! RegistryPolicySet(policySet)
    childActorRef
  }
}

object PolicySetManager {
  def props: Props = Props[PolicySetManager]
}

class PolicySetManager extends Actor {
  import PolicyCollectionManager._

  private val log = Logging.getLogger(context.system, this)

  private val policySet: Option[PolicySet] = None
  private val policyMap = Map.empty[String, ActorRef]

  override def receive: Receive = onMessage(policySet, policyMap)

  private def onMessage(policySet: Option[PolicySet], policyMap: Map[String, ActorRef]): Receive = {
    case RegistryPolicySet(ps) =>
      log.info("Registry policy set: {}", ps)
      val pMap = ps.policies.map(
        policy => policy.target.value -> spawnChild(policy)
      ).toMap
      context.become(onMessage(Some(ps), pMap))
    case FetchPolicySet(target) =>
      policySet match {
        case Some(ps) if ps.target.value == target.value => sender ! Some(ps)
        case _                                           => Behaviors.unhandled
      }
    case command @ FetchPolicy(_, target) =>
      policyMap.get(target.value) match {
        case Some(policy) => policy forward command
        case None         => Behaviors.unhandled
      }
  }

  private def spawnChild(policy: Policy): ActorRef = {
    val childActorRef = context.actorOf(Props[PolicyManager], s"policy-${policy.target.value}")
    childActorRef ! RegistryPolicy(policy)
    childActorRef
  }
}

object PolicyManager {
  def props: Props = Props[PolicyManager]
}

class PolicyManager extends Actor {
  import PolicyCollectionManager._

  private val log = Logging.getLogger(context.system, this)

  private val policy: Option[Policy] = None

  override def receive: Receive = onMessage(policy)

  private def onMessage(policy: Option[Policy]): Receive = {
    case RegistryPolicy(p) =>
      log.info("Registry policy: {}", p)
      context.become(onMessage(Some(p)))
    case FetchPolicy(_, target) =>
      policy match {
        case Some(p) if p.target.value == target.value => sender ! Some(p)
        case _                                         => Behaviors.unhandled
      }
  }
}
