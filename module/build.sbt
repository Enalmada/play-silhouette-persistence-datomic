name := """play-silhouette-persistence-datomic"""

version := "0.1.5"

lazy val module = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.mohiva" %% "play-silhouette" % "4.0.0" % "provided",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "4.0.0" % "provided",
  "com.mohiva" %% "play-silhouette-persistence" % "4.0.0" % "provided",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "4.0.0" % "provided",
  "com.mohiva" %% "play-silhouette-testkit" % "4.0.0" % "test",
  "com.github.dwhjames" %% "datomisca" % "0.7.0" % "provided",
  "com.datomic" % "datomic-free" % "0.9.5544" % "provided",
  "com.github.enalmada" %% "datomisca-dao" % "0.1.4" % "provided",
  "net.codingwell" %% "scala-guice" % "4.1.0" % "provided",
  "com.iheart" %% "ficus" % "1.4.0" % "provided",
  ws,
  specs2 % Test
)

scalacOptions in Test ++= Seq("-Yrangepos")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers ++= Seq(Resolver.bintrayRepo("dwhjames", "maven"), "clojars" at "https://clojars.org/repo")

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// These tests depend on the order.  Should fix that so they are independant.
parallelExecution in Test := false


//*******************************
// Maven settings
//*******************************

publishMavenStyle := true

organization := "com.github.enalmada"

description := "Playframework/Scala/Silhouette/Datomic"

startYear := Some(2016)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra in Global := {
  <url>https://github.com/Enalmada/play-silhouette-persistence-datomic</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:git@github.com:enalmada/play-silhouette-persistence-datomic.git</connection>
      <developerConnection>scm:git:git@github.com:enalmada/play-silhouette-persistence-datomic.git</developerConnection>
      <url>https://github.com/enalmada</url>
    </scm>
    <developers>
      <developer>
        <id>enalmada</id>
        <name>Adam Lane</name>
        <url>https://github.com/enalmada</url>
      </developer>
    </developers>
}

credentials += Credentials(Path.userHome / ".sbt" / "sonatype.credentials")
