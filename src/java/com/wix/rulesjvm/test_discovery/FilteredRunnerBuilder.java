package com.wix.rulesjvm.test_discovery;

import org.junit.runner.Runner;

import java.util.regex.Pattern;

public interface FilteredRunnerBuilder {
  Runner f(Runner a, Class<?> b, Pattern c);
}
