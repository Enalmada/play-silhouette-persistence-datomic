
name := """play-silhouette-persistence-datomic"""

version := "0.3.0"

lazy val module = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.15"
// unmanagedSourceDirectories in Compile += (sourceDirectory in Compile).value / s"scala_${scalaBinaryVersion.value}"

// datomisca not cross compiled with 12
// crossScalaVersions := Seq("2.12.18", "2.13.15")

libraryDependencies ++= {

  val silhouetteVersion = if (scalaVersion.value.startsWith("2.12")) {
    "7.0.7"
  } else {
    "10.0.1"
  }

  val commonDeps = Seq(
    "org.playframework.silhouette" %% "play-silhouette" % silhouetteVersion % "provided",
    "org.playframework.silhouette" %% "play-silhouette-password-bcrypt" % silhouetteVersion % "provided",
    "org.playframework.silhouette" %% "play-silhouette-persistence" % silhouetteVersion % "provided",
    "org.playframework.silhouette" %% "play-silhouette-crypto-jca" % silhouetteVersion % "provided",
    "com.github.enalmada" %% "datomisca" % "0.8.5" % "provided",
    "com.datomic" % "peer" % "1.0.7260" % "provided",
    "com.github.enalmada" %% "datomisca-dao" % "0.2.5" % "provided",
    "net.codingwell" %% "scala-guice" % "6.0.0" % "provided",
    "com.iheart" %% "ficus" % "1.5.2" % "provided",
  )

  commonDeps
}

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
