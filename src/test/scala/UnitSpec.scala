package com.p15x.hntop30

import org.scalamock.proxy.ProxyMockFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest._

abstract class UnitSpec extends FlatSpec with Matchers with
  OptionValues with Inside with Inspectors with MockFactory with ProxyMockFactory
