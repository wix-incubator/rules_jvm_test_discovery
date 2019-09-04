package com.wix.rulesjvm.test_discovery;

import org.junit.Test;

public class TestJunitInnerClass {
  @Test
  public void someTest() {
    System.out.println("passing");
  }

  class SomeHelper {}
} 
