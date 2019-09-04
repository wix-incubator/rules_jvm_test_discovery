package com.wix.rulesjvm.test_discovery;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(JunitCustomRunner.class)
public class JunitCustomRunnerTest {
  @Test
  public void myTest() {
    assertEquals("JunitCustomRunner did not run, check the wiring in JUnitFilteringRunnerBuilder",JunitCustomRunner.message,JunitCustomRunner.EXPECTED_MESSAGE);
  }
}
