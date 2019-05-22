package com.wix.rulesjvm.test_discovery;

import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

public class PrefixSuffixTestDiscoveringSuite extends Suite {
  public PrefixSuffixTestDiscoveringSuite(Class<?> testClass, RunnerBuilder builder) throws InitializationError {
    super(new FilteredRunnerBuilderImpl(builder, new JUnitFilteringRunnerBuilder()), new PrefixSuffixTestDiscoveringSuiteObject().discoverClasses());
  }
}
