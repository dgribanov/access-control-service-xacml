// The Lagom plugin
addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.6.5")

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
