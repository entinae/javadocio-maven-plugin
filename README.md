# JavaDocIO Maven Plugin

> Creates JavaDocs that interlink to [javadoc.io](https://www.javadoc.io).

[![Build Status](https://github.com/entinae/javadocio-maven-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/entinae/javadocio-maven-plugin/actions/workflows/build.yml)
[![Coverage Status](https://coveralls.io/repos/github/entinae/javadocio-maven-plugin/badge.svg)](https://coveralls.io/github/entinae/javadocio-maven-plugin)
[![Javadocs](https://www.javadoc.io/badge/org.entinae.maven/javadocio-maven-plugin.svg)](https://www.javadoc.io/doc/org.entinae.maven/javadocio-maven-plugin)
[![Released Version](https://img.shields.io/maven-central/v/org.entinae.maven/javadocio-maven-plugin.svg)](https://mvnrepository.com/artifact/org.entinae.maven/javadocio-maven-plugin)

The JavaDocIO Maven Plugin creates JavaDocs that interlink to [javadoc.io](https://www.javadoc.io).

### Goals Overview

The JavaDocIO Plugin supports two goals.

* `javadoc:javadoc` Generates documentation for the Java code in either an aggregator or non-aggregator project.
* `javadoc:jar` Bundles the Javadoc documentation for main Java code in an aggregator or non-aggregator aggregator project into a jar.

#### Configuration Parameters

The `javadocio-maven-plugin` supports all of the same configuration parameters as the [`maven-javadoc-plugin`](https://maven.apache.org/plugins/maven-javadoc-plugin/), and provides the following additional parameters:

| **Configuration**              | **Property**               | **Type** | **Use**  | **Description**                                                                                                                                   |
|:-------------------------------|:---------------------------|:---------|:---------|:--------------------------------------------------------------------------------------------------------------------------------------------------|
| `<detectGeneratedSourcePaths>` | detectGeneratedSourcePaths | boolean  | Optional | If `true`, the plugin will detect and include the generated source paths from all subpaths of `target/generated-sources`<br>**Default:** `false`. |

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

### License

This project is licensed under the MIT License - see the [LICENSE.txt](LICENSE.txt) file for details.

[mvn-plugin]: https://img.shields.io/badge/mvn-plugin-lightgrey.svg