package com.example.accesscontrol.api.impl

import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AsyncFeatureSpec

import scala.concurrent.ExecutionContext

abstract class IntegrationSpec extends AsyncFeatureSpec with GivenWhenThen {
  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
}
