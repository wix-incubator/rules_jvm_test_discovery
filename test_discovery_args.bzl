def _serialize_archives_short_path(archives):
    archives_short_path = ""
    for archive in archives:
        archives_short_path += archive.short_path + ","
    return archives_short_path[:-1]  #remove redundant comma

def _get_test_archive_jars(ctx, test_archives):
    flattened_list = []
    for archive in test_archives:
        # rules_scala uses the legacy JavaInfo (java_common.create_provider) and this leads to
        # runtime_output_jars contains more jars than needed
        if hasattr(archive, "scala"):
            jars = [jar.class_jar for jar in archive.scala.outputs.jars]
        else:
            jars = archive[JavaInfo].runtime_output_jars
        flattened_list.extend(jars)
    return flattened_list

def _gen_test_suite_args_based_on_prefixes_and_suffixes(ctx, archives):
    if not ctx.attr.prefixes and not ctx.attr.suffixes:
        fail("**** MISCONFIGURED TEST_DISCOVERY ****\nExpected at least one test prefix or one test suffix. Have you forgotten to specify 'suffixes' or 'prefixs' argument?\n**************************************")
    return """
bazel.discover.classes.archives.file.paths={archives}
bazel.discover.classes.prefixes={prefixes}
bazel.discover.classes.suffixes={suffixes}
bazel.discover.classes.print.discovered={print_discovered}
""".format(archives = archives,
           prefixes = ",".join(ctx.attr.prefixes),
           suffixes = ",".join(ctx.attr.suffixes),
           print_discovered = ctx.attr.print_discovered_classes)


def _test_discovery_args_impl(ctx):
    #TODO check that either prefixes or suffixes is supplied
    archives = _get_test_archive_jars(ctx, ctx.attr.tests_from)

    serialized_archives = _serialize_archives_short_path(archives)

    test_suite = _gen_test_suite_args_based_on_prefixes_and_suffixes(
        ctx,
        serialized_archives,
    )
    ctx.actions.write(ctx.outputs.discovery_args, test_suite)
    return DefaultInfo(
      runfiles=ctx.runfiles(files=[ctx.outputs.discovery_args])
    )

test_discovery_args = rule(
    attrs = {
        "prefixes": attr.string_list(default = []),
        "suffixes": attr.string_list(default = []),
        "print_discovered_classes": attr.bool(
            default = False,
            mandatory = False,
        ),
        "tests_from": attr.label_list(providers = [[JavaInfo]]),
    },
    outputs = {
        "discovery_args" : "%{name}_test_discovery_args"
    },
    implementation = _test_discovery_args_impl,
)
