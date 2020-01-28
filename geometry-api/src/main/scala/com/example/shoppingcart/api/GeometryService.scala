package com.example.shoppingcart.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.Service
import com.lightbend.lagom.scaladsl.api.ServiceCall

trait GeometryService extends Service {

  def addShapes(count: Int): ServiceCall[NotUsed, NotUsed]

  final override def descriptor = {
    import Service._
    named("geometry")
      .withCalls(
        restCall(Method.PUT, "/geometry/:count", addShapes _),
      )
      .withAutoAcl(true)
  }
}
