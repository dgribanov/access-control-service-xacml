package com.example.accesscontrol.admin.ws.api.impl

import com.lightbend.lagom.scaladsl.server._
import com.example.accesscontrol.admin.ws.api.impl.apirest.AccessControlAdminWsServiceImpl
import com.example.accesscontrol.admin.ws.rest.api.AccessControlAdminWsService
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.softwaremill.macwire._
import akka.cluster.sharding.typed.scaladsl.Entity
import com.example.accesscontrol.admin.ws.api.impl.domain.PolicyCollection
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.example.accesscontrol.admin.ws.api.impl.domain.PolicyCollectionSerializerRegistry
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents


class AccessControlAdminLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext): LagomApplication =
    new AccessControlAdminApplication(context) {
      override def serviceLocator: NoServiceLocator.type = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new AccessControlAdminApplication(context) with LagomDevModeComponents

  override def describeService: Some[Descriptor] = Some(readDescriptor[AccessControlAdminWsService])
}

abstract class AccessControlAdminApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with LagomServerComponents
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry =
    PolicyCollectionSerializerRegistry

  // Initialize the sharding for the ShoppingCart aggregate.
  // See https://doc.akka.io/docs/akka/2.6/typed/cluster-sharding.html
  clusterSharding.init(
      Entity(PolicyCollection.typeKey) { entityContext =>
        PolicyCollection(entityContext)
      }
    )

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[AccessControlAdminWsService](wire[AccessControlAdminWsServiceImpl])
}
