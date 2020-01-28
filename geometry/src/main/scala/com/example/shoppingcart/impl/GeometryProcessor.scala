package com.example.shoppingcart.impl

import akka.Done
import akka.stream.scaladsl.Flow
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.example.shoppingcart.impl.Space._
import com.lightbend.lagom.scaladsl.persistence.EventStreamElement

class GeometryProcessor() extends ReadSideProcessor[Event] {

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[Event] =
    () =>
      Flow[EventStreamElement[Event]]
        .map { ese =>
          println(s"geometry-rsp : $ese")
          Done
        }

  override def aggregateTags: Set[AggregateEventTag[Event]] = Event.geometryTag.allTags
}
class ShapeProcessor() extends ReadSideProcessor[Event] {
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[Event] =
    () =>
      Flow[EventStreamElement[Event]]
        .map { ese =>
          println(s"shape-rsp : $ese")
          Done
        }

  override def aggregateTags: Set[AggregateEventTag[Event]] = Set(Event.shapeTag)
}
class FlatProcessor() extends ReadSideProcessor[Event] {
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[Event] =
    () =>
      Flow[EventStreamElement[Event]]
        .map { ese =>
          println(s"flat-shapes-rsp : $ese")
          Done
        }

  override def aggregateTags: Set[AggregateEventTag[Event]] = Set(Event.flatTag)
}
class ColorProcessor() extends ReadSideProcessor[Event] {

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[Event] =
    () =>
      Flow[EventStreamElement[Event]]
        .map { ese =>
          println(s"color-rsp : $ese")
          Done
        }

  override def aggregateTags: Set[AggregateEventTag[Event]] = Event.colorTag.allTags
}
