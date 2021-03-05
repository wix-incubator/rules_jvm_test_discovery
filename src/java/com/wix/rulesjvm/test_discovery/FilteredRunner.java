package com.wix.rulesjvm.test_discovery;

import org.junit.runner.Runner;

import java.util.regex.Pattern;

public interface FilteredRunner {
  Runner replaceTestClassOf(Runner runner, Class<?> testClass, Pattern c);
}
