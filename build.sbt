import com.typesafe.sbt.SbtScalariform._

import scalariform.formatter.preferences._

name := "play-silhouette-seed"

version := "5.0.0"

scalaVersion := "2.12.8"

resolvers := ("Atlassian Releases" at "https://maven.atlassian.com/public/") +: resolvers.value

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers ++= Seq(Resolver.bintrayRepo("thyming", "maven"), "clojars" at "https://clojars.org/repo")

libraryDependencies ++= Seq(
  guice,
  ehcache,
  "com.mohiva" %% "play-silhouette" % "6.0.0-RC1",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "6.0.0-RC1",
  "com.mohiva" %% "play-silhouette-persistence" % "6.0.0-RC1",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "6.0.0-RC1",
  "com.mohiva" %% "play-silhouette-testkit" % "6.0.0-RC1" % "test",
  "com.quartethealth" %% "datomisca" % "0.7.1",
  "com.datomic" % "datomic-free" % "0.9.5544",
  "com.github.enalmada" %% "datomisca-dao" % "0.1.11",
  //"org.webjars" %% "webjars-play" % "2.6.0-M1",
  "net.codingwell" %% "scala-guice" % "4.2.3",
  "com.iheart" %% "ficus" % "1.4.6",
  "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B3-SNAPSHOT",
  "com.typesafe.play" %% "play-mailer" % "7.0.0",
  "com.typesafe.play" %% "play-mailer-guice" % "7.0.0",
  specs2 % Test,
  filters
)

lazy val module = (project in file("module")).enablePlugins(PlayScala)

lazy val root = (project in file(".")).enablePlugins(PlayScala).aggregate(module).dependsOn(module)

routesGenerator := InjectedRoutesGenerator

/*
scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen" // Warn when numerics are widened.
)
*/

//********************************************************
// Scalariform settings
//********************************************************

defaultScalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(FormatXml, false)
  .setPreference(DoubleIndentClassDeclaration, false)
  .setPreference(DanglingCloseParenthesis, Preserve)
