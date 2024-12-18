name := """play-silhouette-persistence-datomic"""

version := "0.1.15"

lazy val module = (project in file(".")).enablePlugins(PlayScala)

val silhouetteVersion = "7.0.7"

scalaVersion := "2.12.18"

// datomisca not cross compiled with 12
// crossScalaVersions := Seq(scalaVersion.value, "2.12.10")

libraryDependencies ++= Seq(
  "io.github.honeycomb-cheesecake" %% "play-silhouette" % silhouetteVersion % "provided",
  "io.github.honeycomb-cheesecake" %% "play-silhouette-password-bcrypt" % silhouetteVersion % "provided",
  "io.github.honeycomb-cheesecake" %% "play-silhouette-persistence" % silhouetteVersion % "provided",
  "io.github.honeycomb-cheesecake" %% "play-silhouette-crypto-jca" % silhouetteVersion % "provided",
  "io.github.honeycomb-cheesecake" %% "play-silhouette-testkit" % silhouetteVersion % "test",
  "com.quartethealth" %% "datomisca" % "0.7.1" % "provided",
  "com.datomic" % "peer" % "1.0.7260" % "provided",
  "com.github.enalmada" %% "datomisca-dao" % "0.1.18" % "provided",
  "net.codingwell" %% "scala-guice" % "4.2.6" % "provided",
  "com.iheart" %% "ficus" % "1.4.7" % "provided",
  ws,
  specs2 % Test
)

scalacOptions in Test ++= Seq("-Yrangepos")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Atlassian Maven" at "https://maven.atlassian.com/content/repositories/atlassian-public/"

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers ++= Seq(Resolver.bintrayRepo("thyming", "maven"), "clojars" at "https://clojars.org/repo")

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

// https://github.com/xerial/sbt-sonatype/issues/30
sources in(Compile, doc) := Seq()
