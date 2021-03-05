load("@bazel_tools//tools/build_defs/repo:jvm.bzl", "jvm_maven_import_external")

def junit_repositories(maven_servers = ["http://central.maven.org/maven2"]):
    jvm_maven_import_external(
        name = "rules_jvm_test_discovery_junit_junit",
        artifact = "junit:junit:4.12",
        artifact_sha256 = "59721f0805e223d84b90677887d9ff567dc534d7c502ca903c0c2b17f05c116a",
        licenses = ["notice"],
        server_urls = maven_servers,
    )
    native.bind(
        name = "rules_jvm_test_discovery/dependency/junit/junit",
        actual = "@rules_jvm_test_discovery_junit_junit//jar",
    )

    jvm_maven_import_external(
        name = "rules_jvm_test_discovery_org_hamcrest_hamcrest_core",
        artifact = "org.hamcrest:hamcrest-core:1.3",
        artifact_sha256 = "66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9",
        licenses = ["notice"],
        server_urls = maven_servers,
    )
    native.bind(
        name = "rules_jvm_test_discovery/dependency/hamcrest/hamcrest_core",
        actual = "@rules_jvm_test_discovery_org_hamcrest_hamcrest_core//jar",
    )
