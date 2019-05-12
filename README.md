# JavaDocIO Maven Plugin

> Creates JavaDocs that interlink to [javadoc.io](https://www.javadoc.io).

[![Build Status](https://travis-ci.org/sevasafris/javadocio-maven-plugin.png)](https://travis-ci.org/sevasafris/javadocio-maven-plugin)
[![Coverage Status](https://coveralls.io/repos/github/sevasafris/javadocio-maven-plugin/badge.svg)](https://coveralls.io/github/sevasafris/javadocio-maven-plugin)
[![Javadocs](https://www.javadoc.io/badge/org.safris.maven/javadocio-maven-plugin.svg)](https://www.javadoc.io/doc/org.safris.maven/javadocio-maven-plugin)
[![Released Version](https://img.shields.io/maven-central/v/org.safris.maven/javadocio-maven-plugin.svg)](https://mvnrepository.com/artifact/org.safris.maven/javadocio-maven-plugin)

The JavaDocIO Maven Plugin creates JavaDocs that interlink to [javadoc.io](https://www.javadoc.io).

### Goals Overview

The CodeGen Plugin has two goals. The goals are bound to the `validate` phase within the Maven Lifecycle, and can be automatically executed during their respective phases with the use of `<extensions>true</extensions>` in the plugin descriptor.

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