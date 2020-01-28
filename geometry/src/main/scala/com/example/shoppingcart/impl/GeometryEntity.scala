package com.example.shoppingcart.impl

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.RetentionCriteria
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.ReplyEffect
import com.example.shoppingcart.impl.Space.ShapeAdded
import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventShards
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTagger
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import play.api.libs.json.Format
import play.api.libs.json._

import scala.collection.immutable.Seq

object Space {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class AddShapes(count: Int, replyTo: ActorRef[Space]) extends Command

  // SHOPPING CART EVENTS
  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.geometryTag
  }

  object Event {
    val geometryBaseTagName = s"geometry-Event"
    val geometryTag: AggregateEventShards[Event] =
      AggregateEventTag.sharded[Event](baseTagName = geometryBaseTagName, numShards = 5)
    val colorBaseTagName = s"color-Event"
    val colorTag: AggregateEventShards[Event] =
      AggregateEventTag.sharded[Event](baseTagName = colorBaseTagName, numShards = 7)
    // everything with more than 2 dimensions has surface, and some are flat while some have volume (3D or more)
    val shapeTag: AggregateEventTag[Event]    = AggregateEventTag[Event]("shape-Event")
    val flatTag: AggregateEventTag[Event] = AggregateEventTag[Event]("surface-Event")
    val volumeTag: AggregateEventTag[Event]   = AggregateEventTag[Event]("volume-Event")
  }

  final case class ShapeAdded(color: String, dimensions: Int) extends Event

  implicit val itemAddedFormat: Format[ShapeAdded] = Json.format

  val empty: Space = Space(0)

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("EuclideanSpace")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, Space] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, Space](
        persistenceId = persistenceId,
        emptyState = Space.empty,
        commandHandler = (space, cmd) => space.applyCommand(cmd),
        eventHandler = (space, evt) => space.applyEvent(evt)
      )
  }

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(
        event => {
          val shapeAdded = event.asInstanceOf[ShapeAdded]
          AkkaTaggerAdapter.fromLagom(entityContext, Event.geometryTag)(event) ++
            AkkaTaggerAdapter.fromLagom(entityContext, Event.colorTag)(event) ++
            // everything with more than 2 dimensions has surface, and some are flat while some have volume
            {
              if (shapeAdded.dimensions >= 2)
                AkkaTaggerAdapter.fromLagom(entityContext, Event.shapeTag)(event) ++ {
                  shapeAdded.dimensions match {
                    case 2 => AkkaTaggerAdapter.fromLagom(entityContext, Event.flatTag)(event)
                    case _ => AkkaTaggerAdapter.fromLagom(entityContext, Event.volumeTag)(event)
                  }
                } else
                Set.empty
            }
        }
      )
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val shoppingCartFormat: Format[Space] = Json.format
}

final case class Space(count: Int) {

  import Space._

  //The shopping cart behavior changes if it's checked out or not. The command handles are different for each case.
  def applyCommand(cmd: Command): ReplyEffect[Event, Space] =
    cmd match {
      case AddShapes(count, replyTo) => onAddShapes(count, replyTo)
    }

  private def onAddShapes(
      quantity: Int,
      replyTo: ActorRef[Space]
  ): ReplyEffect[Event, Space] = {
    // A
    Effect
      .persist((1 to quantity).map(q => ShapeAdded("green", 1 + q % 6))) // at least 1 dimension
      .thenReply(replyTo)(_ => this)
  }

  def applyEvent(evt: Event): Space =
    evt match {
      case ShapeAdded(color, sides) => copy(count = count + 1) // not changing the state
    }

}

object GeometrySerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[Space],
    JsonSerializer[ShapeAdded],
  )
}
