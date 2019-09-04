package com.wix.rulesjvm.test_discovery;

import org.junit.Test;
import org.junit.runner.Runner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.TestClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


class FilteredRunnerBuilder extends RunnerBuilder {

  private RunnerBuilder builder;
  private FilteredRunner filteredRunner;

  public FilteredRunnerBuilder(RunnerBuilder builder, FilteredRunner filteredRunner) {
    this.builder = builder;
    this.filteredRunner = filteredRunner;
  }

  // Defined by --test_filter bazel flag.
  private Optional<Pattern> maybePattern = Optional.ofNullable(System.getenv("TESTBRIDGE_TEST_ONLY")).map(Pattern::compile);

  @Override
  public Runner runnerForClass(Class<?> testClass) throws Throwable {
    Runner runner = builder.runnerForClass(testClass);

    return maybePattern.map(p -> filteredRunner.replaceTestClassOf(runner, testClass, p)).orElse(runner);
  }
}

class FilteredTestClass extends TestClass {
  private Pattern pattern;

  public FilteredTestClass(Class<?> testClass, Pattern pattern) {
    super(testClass);

    this.pattern = pattern;
  }

  @Override
  public List<FrameworkMethod> getAnnotatedMethods(Class<? extends Annotation> aClass) {
    List<FrameworkMethod> methods = super.getAnnotatedMethods(aClass);
    if (aClass == Test.class)
      return methods.stream().filter(method -> methodMatchesPattern(method, pattern)).collect(Collectors.toList());
    else return methods;
  }

  private Boolean methodMatchesPattern(FrameworkMethod method, Pattern pattern) {
    String testCase = method.getDeclaringClass().getName() + "#" + method.getName();
    return pattern.matcher(testCase).find();
  }
}

class FilteredJUnitRunner implements FilteredRunner {
  private final String TestClassFieldPreJUnit4_12 = "fTestClass";
  private final String TestClassField = "testClass";

  @Override
  public Runner replaceTestClassOf(Runner runner, Class<?> testClass, Pattern pattern) {
    return replaceRunnerTestClass(runner, testClass, pattern);
  }


  private Runner replaceRunnerTestClass(Runner runner, Class<?> testClass, Pattern pattern) {
    allFieldsOf(runner.getClass())
            .stream()
            .filter(f -> f.getName().equals(TestClassField) || f.getName().equals(TestClassFieldPreJUnit4_12)).findFirst()
            .ifPresent(field -> {
              field.setAccessible(true);
              try {
                field.set(runner, new FilteredTestClass(testClass, pattern));
              } catch (IllegalAccessException e) {
                e.printStackTrace();
              }
            });
    return runner;
  }

  private static List<Field> allFieldsOf(Class<?> clazz) {
    return supers(clazz).stream().flatMap(c -> Arrays.stream(c.getDeclaredFields())).collect(Collectors.toList());
  }

  private static List<Class<?>> supers(Class<?> cl) {
    if (cl == null) return new LinkedList<Class<?>>();
    else {
      List<Class<?>> classList = supers(cl.getSuperclass());
      classList.add(0, cl);
      return classList;
    }
  }


}
