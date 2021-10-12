package com.example.accesscontrol.api.impl.infrastructure

import com.example.accesscontrol.api.impl.AccessControlModule
import com.google.inject.{Guice, Injector, Module}

object ServiceInjectorFactory {
  var module: Module = new AccessControlModule()

  def buildServiceInjector(): Injector = {
    Guice.createInjector(module)
  }
}
