load("@rules_jvm_test_discovery//:test_discovery_args.bzl", "test_discovery_args")

def java_test_discovery(name, 
    tests_from,
    suffixes = None,
    prefixes = None, 
    print_discovered_classes = False, 
    **kwargs):

    discovery_name = "%s-discovery-args" % name
    test_discovery_args(
        name = discovery_name,
        prefixes = prefixes,
        suffixes = suffixes,
        print_discovered_classes = print_discovered_classes,
        tests_from = tests_from,
    )

    user_jvm_flags = kwargs.pop("jvm_flags", [])
    user_data = kwargs.pop("data", [])
    user_runtime_deps = kwargs.pop("runtime_deps", [])
    suite_class = "com.wix.rulesjvm.test_discovery.DiscoveredTestSuite"
    suite_label = "@rules_jvm_test_discovery//src/java/com/wix/rulesjvm/test_discovery"
    native.java_test(
        name = name,
        jvm_flags = user_jvm_flags + [
            "-Dbazel.test_suite=%s" % suite_class,
            "-Dbazel.discover.classes.args.path=$(location :%s)" % discovery_name,
        ],
        data = user_data + [":" + discovery_name],
        runtime_deps = user_runtime_deps + tests_from + [suite_label,
                                                         "@rules_jvm_test_discovery//src/java/com/wix/rulesjvm/test_discovery:bazel_test_runner_deploy"],
        use_testrunner = False,
        main_class = "com.google.testing.junit.runner.BazelTestRunner",
        **kwargs
    )
