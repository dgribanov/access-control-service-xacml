ThisBuild / organization := "com.example"
ThisBuild / version := "1.1-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
ThisBuild / scalaVersion := "2.13.0"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.4.1" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.2.10" % Test

lazy val `hello-world` = (project in file("."))
  .aggregate(`access-control-rest-api`, `access-control-api-impl`)

lazy val `access-control-rest-api` = (project in file("access-control-rest-api"))
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
      macwire,
      scalaTest
    )
  )
  .dependsOn(`access-control-rest-api`)

/*lazy val `hello-world-api` = (project in file("hello-world-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `hello-world-impl` = (project in file("hello-world-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`hello-world-api`)

lazy val `hello-world-stream-api` = (project in file("hello-world-stream-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `hello-world-stream-impl` = (project in file("hello-world-stream-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .dependsOn(`hello-world-stream-api`, `hello-world-api`)*/
