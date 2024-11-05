val scala3Version = "3.5.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ZIO http test",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    // Build dependecies
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-http" % "3.0.1"
    ),
    // Test dependecies
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.0.0" % Test
    )
  )
