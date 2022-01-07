package com.example.accesscontrol.api.impl.domain

object PolicyCollectionExecutable {
  def convert: PartialFunction[PolicyCollection, PolicyCollectionExecutable] = {
    case pc: PolicyCollection => PolicyCollectionExecutable(pc.id, pc.version, pc.policySets map PolicySetExecutable.convert)
  }
}

case class PolicyCollectionExecutable(id: String, version: String, policySets: Array[PolicySetExecutable]) extends PolicyCollection
