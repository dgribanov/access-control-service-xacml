package com.example.accesscontrol.api.impl

import com.google.inject.{Guice, Module}
import net.codingwell.scalaguice.InjectorExtensions._
import scala.reflect.runtime.universe._

final class ServiceInjector(private implicit val module: Module) {
  private val injector = Guice.createInjector(module)

  def inject[T](implicit tag: TypeTag[T]): T = injector.instance[T]
}
