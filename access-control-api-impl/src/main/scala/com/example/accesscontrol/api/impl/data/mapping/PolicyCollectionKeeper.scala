package com.example.accesscontrol.api.impl.data.mapping

import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging

object PolicyCollectionKeeper {
  def props = Props[PolicyCollectionKeeper]

  sealed trait Command
  case class RegistryPolicyCollection(policyCollection: PolicyCollectionSerializable) extends Command
  case class FetchPolicyCollection() extends Command
}

class PolicyCollectionKeeper extends Actor {
  import PolicyCollectionKeeper._
  private val log = Logging.getLogger(context.system, this)

  private var policyCollection: Option[PolicyCollectionSerializable] = None

  override def receive = {
    case RegistryPolicyCollection(p) =>
      log.info("Registry policy collection: {}", p)
      policyCollection = Some(p)
      this
    case _: FetchPolicyCollection =>
      sender ! policyCollection
      this
  }
}
