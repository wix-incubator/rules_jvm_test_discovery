package com.wix.rulesjvm.test_discovery;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The test running and discovery mechanism works in the following manner:
 *   - Bazel rule executes a JVM application to run tests (currently `JUnitCore`) and asks it to run
 *     the `DiscoveredTestSuite` suite.
 *   - When JUnit tries to run it, it uses the `PrefixSuffixTestDiscoveringSuite` runner
 *     to know what tests exist in the suite.
 *   - We know which tests to run by examining the entries of the target's archive.
 *   - The archive's path is passed in a system property ("bazel.discover.classes.archive.file.path").
 *   - The entries of the archive are filtered to keep only classes
 *   - Of those we filter again and keep only those which match either of the prefixes/suffixes supplied.
 *   - Prefixes are supplied as a comma separated list. System property ("bazel.discover.classes.prefixes")
 *   - Suffixes are supplied as a comma separated list. System property ("bazel.discover.classes.prefixes")
 *   - We iterate over the remaining entries and format them into classes.
 *   - At this point we tell JUnit (via the `RunnerBuilder`) what are the discovered test classes.
 *   - W.R.T. discovery semantics this is similar to how maven surefire/failsafe plugins work.
 *   - For debugging purposes one can ask to print the list of discovered classes.
 *   - This is done via an `print_discovered_classes` attribute.
 *   - The attribute is sent via "bazel.discover.classes.print.discovered"
 *
 * Additional references:
 *   - http://junit.org/junit4/javadoc/4.12/org/junit/runner/RunWith.html
 *   - http://junit.org/junit4/javadoc/4.12/org/junit/runners/model/RunnerBuilder.html
 *   - http://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html
 */
@RunWith(PrefixSuffixTestDiscoveringSuite.class)
class DiscoveredTestSuite {
}

class PrefixSuffixTestDiscoveringSuiteObject {
  Class<?>[] discoverClasses() {
    String[] archives = archivesPath().split(",");
    Class<?>[] classes = Arrays.stream(archives).flatMap(this::discoverClassesIn).toArray(Class<?>[]::new);
    if (classes.length == 0)
      throw new IllegalStateException("Was not able to discover any classes " +
              "for archives=" + archivesPath() + ", " +
              "prefixes=" + prefixes() + ", " +
              "suffixes=" + suffixes());
    return classes;
  }

  private Properties testDiscoveryArgs = loadTestDiscoveryArgs();

  private final Properties loadTestDiscoveryArgs() {
    String testDiscoveryArgsPath = System.getProperty("bazel.discover.classes.args.path");
    Properties properties = new Properties();
    try {
      properties.load(Files.newBufferedReader(Paths.get(testDiscoveryArgsPath)));
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    return properties;
  }

  private Stream<Class<?>> discoverClassesIn(String archivePath) {
    JarInputStream archive = archiveInputStream(archivePath);
    Class<?>[] classes = discoverClasses(archive, prefixes(), suffixesWithClassSuffix());
    try {
      archive.close();
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    if (printDiscoveredClasses()) {
      System.out.println("Discovered classes:");
      Arrays.stream(classes).forEach(c -> System.out.println(c.getName()));
    }
    return Arrays.stream(classes);
  }

  private Class<?> forName(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private Class<?>[] discoverClasses(JarInputStream archive,
                                     Set<String> prefixes,
                                     Set<String> suffixes) {
    return matchingEntries(archive, prefixes, suffixes).stream()
            .map(this::dropFileSuffix)
            .map(this::fileToClassFormat)
            .filter(c -> !innerClasses(c))
            .map(this::forName)
            .filter(this::concreteClasses)
            .filter(this::containsTests)
            .toArray(Class<?>[]::new);
  }

  private List<String> matchingEntries(JarInputStream archive,
                                       Set<String> prefixes,
                                       Set<String> suffixes) {
    Stream<String> stream = entries(archive)
            .stream()
            .filter(this::isClass)
            .filter(entry -> endsWith(suffixes, entry) || startsWith(prefixes, entry));
    List<String> list = stream.collect(Collectors.toList());
    return list;
  }

  private Boolean startsWith(Set<String> prefixes, String entry) {
    String entryName = entryFileName(entry);
    return prefixes.stream().anyMatch(entryName::startsWith);
  }

  private Boolean endsWith(Set<String> suffixes, String entry) {
    String entryName = entryFileName(entry);
    return suffixes.stream().anyMatch(entryName::endsWith);
  }


  private String entryFileName(String entry) {
    return new File(entry).getName();
  }

  private String dropFileSuffix(String classEntry) {
    return classEntry.split("\\.")[0];
  }


  private String fileToClassFormat(String classEntry) {
    return classEntry.replace('/', '.');
  }

  private Boolean isClass(String entry) {
    return entry.endsWith(".class");
  }

  private List<String> entries(JarInputStream jarInputStream) {
    LinkedList<String> entries = new LinkedList<>();

    JarEntry entry;
    while (true) {
      try {
        if ((entry = jarInputStream.getNextJarEntry()) == null) break;
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
      entries.add(entry.getName());
    }

    return entries;
  }

  private JarInputStream archiveInputStream(String archivePath) {
    try {
      return new JarInputStream(new FileInputStream(archivePath));
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private String archivesPath() {
    return testDiscoveryArgs.getProperty("bazel.discover.classes.archives.file.paths"); //this is set by test_discovery_args rule in test_discovery_args.bzl
  }

  private java.util.Set<String> suffixesWithClassSuffix() {
    Stream<String> stream = suffixes().stream().map((suffix) -> suffix + ".class");
    return stream.collect(Collectors.toSet());
  }

  private java.util.Set<String> suffixes() {
    return parseProperty(testDiscoveryArgs.getProperty("bazel.discover.classes.suffixes"));
  }

  private Set<String> prefixes() {
    return parseProperty(testDiscoveryArgs.getProperty("bazel.discover.classes.prefixes"));
  }

  private Set<String> parseProperty(String potentiallyEmpty) {
    if (potentiallyEmpty.trim().isEmpty())
      return new HashSet<String>();
    else
      return new HashSet<>(Arrays.asList(potentiallyEmpty.split(",")));

  }

  private Boolean printDiscoveredClasses() {
    return Boolean.valueOf(testDiscoveryArgs.getProperty("bazel.discover.classes.print.discovered"));
  }

  private Boolean concreteClasses(Class<?> testClass) {
    return !Modifier.isAbstract(testClass.getModifiers());
  }

  private Boolean innerClasses(String testClassName) {
    return testClassName.contains("$");
  }

  private Boolean containsTests(Class<?> testClass) {
    return annotatedWithRunWith(testClass) || hasTestAnnotatedMethodsInClassHierarchy(testClass);
  }

  private Boolean annotatedWithRunWith(Class<?> testClass) {
    return testClass.getAnnotation(runWithAnnotation) != null;
  }

  private Boolean hasTestAnnotatedMethodsInClassHierarchy(Class<?> testClass) {
    if (testClass == null) return false;
    if (hasTestAnnotatedMethodsInCurrentClass(testClass)) return true;

    return hasTestAnnotatedMethodsInClassHierarchy(testClass.getSuperclass());
  }

  private Boolean hasTestAnnotatedMethodsInCurrentClass(Class<?> testClass) {
    return Arrays.stream(testClass.getDeclaredMethods())
            .anyMatch(method ->
                    Arrays.stream(method.getAnnotations()).anyMatch(annotation ->
                            testAnnotation.isAssignableFrom(annotation.annotationType()))
            );
  }

  private Class<RunWith> runWithAnnotation = RunWith.class;
  private Class<Test> testAnnotation = Test.class;
}