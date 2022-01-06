package com.example.accesscontrol.admin.ws.api.impl.domain

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.ReplyEffect
import akka.persistence.typed.scaladsl.RetentionCriteria

import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventShards
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTagger
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry

import play.api.libs.json.Format
import play.api.libs.json._


object PolicyCollection {
  final val version: String = "v1"

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class RegisterPolicySet(policySet: PolicySetSerializable, replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class PolicySetRegistered(policySet: PolicySetSerializable) extends Event

  // Events get stored and loaded from the database, hence a JSON format
  //  needs to be declared so that they can be serialized and deserialized.
  implicit val policySetRegisteredFormat: Format[PolicySetRegistered] = Json.format


  final case class Summary(policySet: PolicySetSerializable)
  sealed trait Confirmation

  final case class Accepted(summary: Summary) extends Confirmation
  final case class Rejected(reason: String) extends Confirmation

  implicit val summaryFormat: Format[Summary]               = Json.format
  implicit val confirmationAcceptedFormat: Format[Accepted] = Json.format
  implicit val confirmationRejectedFormat: Format[Rejected] = Json.format
  implicit val confirmationFormat: Format[Confirmation] = new Format[Confirmation] {
    override def reads(json: JsValue): JsResult[Confirmation] = {
      if ((json \ "reason").isDefined)
        Json.fromJson[Rejected](json)
      else
        Json.fromJson[Accepted](json)
    }

    override def writes(o: Confirmation): JsValue = {
      o match {
        case acc: Accepted => Json.toJson(acc)
        case rej: Rejected => Json.toJson(rej)
      }
    }
  }

  val empty: PolicyCollection = PolicyCollection(policySets = Map.empty)

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("PolicyCollection")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, PolicyCollection] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, PolicyCollection](
        persistenceId = persistenceId,
        emptyState = PolicyCollection.empty,
        commandHandler = (policyCollection, cmd) => policyCollection.applyCommand(cmd),
        eventHandler = (policyCollection, evt) => policyCollection.applyEvent(evt)
      )
  }

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  /**
   * The aggregate get snapshoted every configured number of events. This
   * means the state gets stored to the database, so that when the entity gets
   * loaded, you don't need to replay all the events, just the ones since the
   * snapshot. Hence, a JSON format needs to be declared so that it can be
   * serialized and deserialized when storing to and from the database.
   */
  implicit val policyCollectionFormat: Format[PolicyCollection] = Json.format
}

final case class PolicyCollection(policySets: Map[String, PolicySetSerializable]) {
  import PolicyCollection._

  def applyCommand(command: Command): ReplyEffect[Event, PolicyCollection] =
    command match {
      case RegisterPolicySet(policySet, replyTo) => onRegisterPolicySet(policySet, replyTo)
    }

  private def onRegisterPolicySet(
    policySet: PolicySetSerializable,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, PolicyCollection] = {
    if (policySets.contains(policySet.target.value))
      Effect.reply(replyTo)(Rejected(s"PolicySet with target '${policySet.target.value}' was already added to policy collection"))
    else
      Effect
        .persist(PolicySetRegistered(policySet))
        .thenReply(replyTo)(policyCollection => Accepted(Summary(policySet)))
  }

  def applyEvent(event: Event): PolicyCollection =
    event match {
      case PolicySetRegistered(policySet) => onPolicySetRegistered(policySet)
    }

  private def onPolicySetRegistered(policySet: PolicySetSerializable): PolicyCollection =
    copy(policySets = policySets + (policySet.target.value -> policySet))
}

/**
 * Akka serialization, used by both persistence and remoting, needs to have
 * serializers registered for every type serialized or deserialized. While it's
 * possible to use any serializer you want for Akka messages, out of the box
 * Lagom provides support for JSON, via this registry abstraction.
 *
 * The serializers are registered here, and then provided to Lagom in the
 * application loader.
 */
object PolicyCollectionSerializerRegistry extends JsonSerializerRegistry {

  import PolicyCollection._

  override def serializers: Seq[JsonSerializer[_]] = Seq(
    // state and events can use play-json, but commands should use jackson because of ActorRef[T] (see application.conf)
    JsonSerializer[PolicyCollection],
    JsonSerializer[PolicySetRegistered],
    // the replies use play-json as well
    JsonSerializer[Summary],
    JsonSerializer[Confirmation],
    JsonSerializer[Accepted],
    JsonSerializer[Rejected],
  )
}
