package com.example.accesscontrol.api.impl.domain

import play.api.libs.json.JsValue

trait PolicyRepository {
  def fetchPolicyCollection: JsValue
}
