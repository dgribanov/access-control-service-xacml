package com.example.accesscontrol.api.impl.infrastructure

import com.example.accesscontrol.api.impl.AccessControlModule
import com.google.inject.{Guice, Injector, Module}
import net.codingwell.scalaguice.InjectorExtensions._
import scala.reflect.runtime.universe._

object ServiceInjector {
  var module: Module = new AccessControlModule()

  def inject[T](implicit tag: TypeTag[T]): T = buildServiceInjector.instance[T]

  private def buildServiceInjector: Injector = Guice.createInjector(module)
}
