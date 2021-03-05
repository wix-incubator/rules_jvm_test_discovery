package com.wix.rulesjvm.test_discovery;

import org.junit.rules.TestRule;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.util.List;

public class JunitCustomRunner extends BlockJUnit4ClassRunner {

  public JunitCustomRunner(Class<?> aClass) throws InitializationError {
    super(aClass);
  }

  public static final String EXPECTED_MESSAGE = "Hello from getTestRules!";
  public static String message;

  @Override
  protected List<TestRule> getTestRules(Object target) {
    message = EXPECTED_MESSAGE;
    return super.getTestRules(target);
  }
}
