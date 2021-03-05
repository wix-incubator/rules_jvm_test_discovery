package com.wix.rulesjvm.test_discovery;

import org.junit.Test;

abstract class ContractTest {
  @Test
  public void abstractTest() {
    System.out.println("Test Method From Parent");
  }
}

public class ConcreteImplementationTest extends ContractTest {}
