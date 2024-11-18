// import com.typesafe.sbt.SbtScalariform._
// import scalariform.formatter.preferences._

name := "play-silhouette-seed"

version := "5.0.0"

scalaVersion := "2.13.15"

resolvers := ("Atlassian Releases" at "https://maven.atlassian.com/public/") +: resolvers.value

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers ++= Seq(Resolver.bintrayRepo("thyming", "maven"), "clojars" at "https://clojars.org/repo")

val silhouetteVersion = "9.0.1"

libraryDependencies ++= Seq(
  guice,
  ehcache,
  "org.playframework.silhouette" %% "play-silhouette" % silhouetteVersion,
  "org.playframework.silhouette" %% "play-silhouette-password-bcrypt" % silhouetteVersion,
  "org.playframework.silhouette" %% "play-silhouette-persistence" % silhouetteVersion,
  "org.playframework.silhouette" %% "play-silhouette-crypto-jca" % silhouetteVersion,
  "org.playframework.silhouette" %% "play-silhouette-testkit" % silhouetteVersion % Test,
  "com.github.enalmada" %% "datomisca" % "0.8.5",
  "com.datomic" % "peer" % "1.0.7260",
  "com.github.enalmada" %% "datomisca-dao" % "0.2.5",
  //"org.webjars" %% "webjars-play" % "2.6.0-M1",
  "net.codingwell" %% "scala-guice" % "4.2.11",
  "com.iheart" %% "ficus" % "1.5.2",
  "com.adrianhurt" %% "play-bootstrap" % "1.6.1-P28-B4",
  "com.typesafe.play" %% "play-mailer" % "9.1.0",
  "com.typesafe.play" %% "play-mailer-guice" % "9.1.0",
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
/*
defaultScalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(FormatXml, false)
  .setPreference(DoubleIndentClassDeclaration, false)
  .setPreference(DanglingCloseParenthesis, Preserve)
*/
