package com.example.accesscontrol.api.impl.domain

import com.example.accesscontrol.admin.ws.rest.api.AccessControlAdminWsService

trait PolicyRecorder {
  def handlePolicyEvents(accessControlAdminWsService: AccessControlAdminWsService): Unit
  def registerPolicyCollection(policyCollection: PolicyCollection): Unit
}
