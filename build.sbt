ThisBuild / organization := "com.example"
ThisBuild / version := "1.1-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
ThisBuild / scalaVersion := "2.13.0"
ThisBuild / lagomCassandraCleanOnStart := true

val macwire = "com.softwaremill.macwire" %% "macros" % "2.4.1" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.2.10" % Test
val scalaGuice = "net.codingwell" %% "scala-guice" % "5.0.2"

lazy val akkaManagementVersion = "1.1.3"
val akkaManagement = "com.lightbend.akka.management" %% "akka-management" % akkaManagementVersion
val akkaManagementHttp = "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion
val akkaClusterBootstrap = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion
val akkaDiscoveryK8s = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion

val akkaManagementDeps = Seq(akkaManagement, akkaManagementHttp, akkaClusterBootstrap, akkaDiscoveryK8s)

enablePlugins(JavaAppPackaging, DockerPlugin)

ThisBuild / dynverSeparator := "-"

dockerBaseImage := "adoptopenjdk:11-jre-hotspot"
dockerUsername := sys.props.get("docker.username")
dockerRepository := sys.props.get("docker.registry")
dockerUpdateLatest := true

lazy val `access-control` = (project in file("."))
  .aggregate(
    `access-control-rest-api`,
    `access-control-api-impl`,
    `access-control-admin-ws-rest-api`,
    `access-control-admin-ws-api-impl`
  )

lazy val `access-control-rest-api` = (project in file("access-control-rest-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `access-control-admin-ws-rest-api` = (project in file("access-control-admin-ws-rest-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `access-control-api-impl` = (project in file("access-control-api-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslTestKit,
      lagomScaladslKafkaClient,
      macwire,
      scalaTest,
      scalaGuice
    ) ++ akkaManagementDeps
  )
  .dependsOn(`access-control-rest-api`, `access-control-admin-ws-rest-api`)

lazy val `access-control-admin-ws-api-impl` = (project in file("access-control-admin-ws-api-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslTestKit,
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      macwire,
      scalaTest
    ) ++ akkaManagementDeps
  )
  .dependsOn(`access-control-admin-ws-rest-api`)
