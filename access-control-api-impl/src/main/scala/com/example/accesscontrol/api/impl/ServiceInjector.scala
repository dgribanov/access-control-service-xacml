package com.example.accesscontrol.api.impl

import com.google.inject.{Guice, Injector, Module}
import net.codingwell.scalaguice.InjectorExtensions._
import scala.reflect.runtime.universe._

object ServiceInjector {
  def inject[T](implicit tag: TypeTag[T], module: Module): T = buildServiceInjector.instance[T]

  private def buildServiceInjector(implicit module: Module): Injector = Guice.createInjector(module)
}
