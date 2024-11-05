import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

name := "play-silhouette-seed"

version := "6.0.0"

scalaVersion := "2.13.11"

resolvers := ("Atlassian Releases" at "https://maven.atlassian.com/public/") +: resolvers.value

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers ++= Seq(Resolver.bintrayRepo("thyming", "maven"), "clojars" at "https://clojars.org/repo")

libraryDependencies ++= Seq(
  guice,
  ehcache,
  "io.github.honeycomb-cheesecake" %% "play-silhouette" % "7.0.0",
  "io.github.honeycomb-cheesecake" %% "play-silhouette-password-bcrypt" % "7.0.0",
  "io.github.honeycomb-cheesecake" %% "play-silhouette-persistence" % "7.0.0",
  "io.github.honeycomb-cheesecake" %% "play-silhouette-crypto-jca" % "7.0.0",
  "io.github.honeycomb-cheesecake" %% "play-silhouette-testkit" % "7.0.0" % "test",
  "com.github.enalmada" %% "datomisca" % "0.8.0",
  "com.datomic" % "peer" % "0.9.5697",
  "com.github.enalmada" %% "datomisca-dao" % "0.2.1",
  //"org.webjars" %% "webjars-play" % "2.6.0-M1",
  "net.codingwell" %% "scala-guice" % "4.2.11",
  "com.iheart" %% "ficus" % "1.5.0",
  "com.adrianhurt" %% "play-bootstrap" % "1.6.1-P28-B3",
  "com.typesafe.play" %% "play-mailer" % "8.0.1",
  "com.typesafe.play" %% "play-mailer-guice" % "8.0.1",
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
