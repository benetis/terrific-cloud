import Dependencies._

lazy val root = (project in file("."))
  .settings(
    scalaVersion := "2.13.6",
    scalacOptions ~= (_.filterNot(Set("-Xfatal-warnings"))),
    libraryDependencies ++= Seq(
      L.http4s("ember-server"),
      L.http4s("ember-client"),
      L.http4s("circe"),
      L.http4s("dsl"),
      L.circe,
      L.squants,
      L.logback,
      L.pureConfig,
      T.munit,
      T.http4sMunit,
      C.betterMonadicFor,
      C.kindProjector
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
