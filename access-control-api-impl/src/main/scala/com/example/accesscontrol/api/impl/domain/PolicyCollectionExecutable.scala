package com.example.accesscontrol.api.impl.domain

object PolicyCollectionExecutable {
  def convert: PartialFunction[PolicyCollection, PolicyCollectionExecutable] = {
    case pc: PolicyCollection => PolicyCollectionExecutable(pc.policySets map PolicySetExecutable.convert)
  }
}

case class PolicyCollectionExecutable(policySets: Array[PolicySetExecutable]) extends PolicyCollection
