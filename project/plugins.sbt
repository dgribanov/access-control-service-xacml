// The Lagom plugin
addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.6.5")

// https://github.com/sbt/sbt-native-packager
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.7")

// https://github.com/dwijnand/sbt-dynver
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

/**
 * Sbt plugin for explore dependency tree of project
 * @see https://github.com/sbt/sbt-dependency-graph
 * @see https://www.baeldung.com/scala/sbt-dependency-tree
 *
 * main commands:
 * - sbt dependencyTree
 * - sbt dependencyBrowseTree
 * - sbt dependencyBrowseGraph
 */
addDependencyTreePlugin
