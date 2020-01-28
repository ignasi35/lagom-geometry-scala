package com.example.shoppingcart.impl

import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents
import akka.cluster.sharding.typed.scaladsl.Entity
import com.example.shoppingcart.api.GeometryService
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry

import scala.concurrent.ExecutionContext

class GeometryLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new GeometryApplication(context) with AkkaDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new GeometryApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[GeometryService])
}

trait GeometryComponents
    extends LagomServerComponents
    with SlickPersistenceComponents
    with HikariCPComponents
    with AhcWSComponents {

  implicit def executionContext: ExecutionContext

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer =
    serverFor[GeometryService](wire[GeometryServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry =
    GeometrySerializerRegistry

  readSide.register(wire[GeometryProcessor])
  readSide.register(wire[ColorProcessor])
  readSide.register(wire[ShapeProcessor])
  readSide.register(wire[FlatProcessor])

  // Initialize the sharding for the Geometry aggregate.
  // See https://doc.akka.io/docs/akka/2.6/typed/cluster-sharding.html
  clusterSharding.init(
    Entity(Space.typeKey) { entityContext =>
      Space(entityContext)
    }
  )
}

abstract class GeometryApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with GeometryComponents {}
