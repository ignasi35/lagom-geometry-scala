package com.example.shoppingcart.impl

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.ExecutionContext
import akka.cluster.sharding.typed.scaladsl.ClusterSharding

import scala.concurrent.duration._
import akka.util.Timeout
import akka.cluster.sharding.typed.scaladsl.EntityRef
import com.example.shoppingcart.api.GeometryService
import com.example.shoppingcart.impl.Space._

import scala.concurrent.Future

/**
 * Implementation of the `ShoppingCartService`.
 */
class GeometryServiceImpl(
    clusterSharding: ClusterSharding,
    persistentEntityRegistry: PersistentEntityRegistry
)(implicit ec: ExecutionContext)
    extends GeometryService {

  /**
   * Looks up the shopping cart entity for the given ID.
   */
  private def entityRef(id: String): EntityRef[Command] =
    clusterSharding.entityRefFor(Space.typeKey, id)

  implicit val timeout = Timeout(5.seconds)

  override def addShapes(count: Int): ServiceCall[NotUsed, NotUsed] = ServiceCall { _ =>
    Future.sequence(
      (0 to count).map(
        _ =>
          entityRef("euclidean+" + System.currentTimeMillis())
            .ask(reply => AddShapes(count, reply))
            .map(_ => NotUsed)
      ).toSeq
    ).map(_.head)
  }

}
