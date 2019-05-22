package com.wix.rulesjvm.test_discovery;

import org.junit.Test;

import java.lang.management.ManagementFactory;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class JunitWithDepsTest {

  @Test
  public void hasCompileTimeDependencies() {
    System.out.println(new JUnitCompileTimeDep().hello);
  }

  @Test
  public void hasRuntimeDependencies() throws ClassNotFoundException {
    Class.forName("com.wix.rulesjvm.test_discovery.JUnitRuntimeDep");
  }

  @Test
  public void supportsCustomJVMArgs() {
    assertThat(ManagementFactory.getRuntimeMXBean().getInputArguments(),
            hasItem("-XX:HeapDumpPath=/some/custom/path"));
  }

}

class ClassCoveringRegressionFromTakingAllClassesInArchive {}