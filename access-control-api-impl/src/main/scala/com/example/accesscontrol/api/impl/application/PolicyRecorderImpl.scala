package com.example.accesscontrol.api.impl.application

import com.example.accesscontrol.admin.ws.rest.api.{
  AccessControlAdminWsService,
  PolicyCollectionRegisteredEvent,
  PolicyEvent,
  PolicyCollection => AdminPolicyCollection
}
import com.example.accesscontrol.api.impl.domain.{PolicyCollection, PolicyRecorder, PolicyRetrievalPoint}
import com.example.accesscontrol.api.impl.data.mapping.PolicyCollectionSerializable

import akka.Done
import akka.stream.scaladsl.Flow
import play.api.libs.json.{JsSuccess, Json}

import scala.language.implicitConversions
import javax.inject.Inject

final case class PolicyRecorderImpl @Inject()(policyRetrievalPoint: PolicyRetrievalPoint) extends PolicyRecorder {
  def handlePolicyEvents(accessControlAdminWsService: AccessControlAdminWsService): Unit =
    accessControlAdminWsService.policyEventsTopic.subscribe.atLeastOnce(Flow[PolicyEvent].map { event =>
      event match {
        case PolicyCollectionRegisteredEvent(policyCollection) => registerPolicyCollection(policyCollection)
      }
      Done
    })

  def registerPolicyCollection(policyCollection: PolicyCollection): Unit =
    policyRetrievalPoint.registerPolicyCollection(policyCollection)

  implicit def convertAdminPolicyCollectionToDomainPolicyCollection(
    adminPolicyCollection: AdminPolicyCollection
  ): PolicyCollection = Json.fromJson[PolicyCollectionSerializable](Json.toJson(adminPolicyCollection)) match {
    case JsSuccess(policyCollection, _) => policyCollection
  }
}
