lazy val baseName       = "AudioWidgets"
lazy val baseNameL      = baseName.toLowerCase

lazy val projectVersion = "2.3.0-SNAPSHOT"
lazy val mimaVersion    = "2.3.0" // used for migration-manager

lazy val commonSettings = Seq(
  version             := projectVersion,
  organization        := "de.sciss",
  description         := "Specialized Swing widgets for audio applications in Scala",
  homepage            := Some(url(s"https://git.iem.at/sciss/$baseName")),
  licenses            := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
  scalaVersion        := "2.13.4",
  crossScalaVersions  := Seq("3.0.0-M1", "2.13.4", "2.12.12"),
  scalacOptions      ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xlint", "-Xsource:2.13"),
  initialCommands in console := """
    |import de.sciss.audiowidgets._""".stripMargin
) ++ publishSettings

// ---- dependencies ----

lazy val deps = new {
  val main = new {
    val desktop     = "0.11.2"
    val span        = "2.0.0"
    val raphael     = "1.0.7"
    val swingPlus   = "0.5.0"
  }
  val test = new {
    val submin      = "0.3.4"
  }
}

lazy val testSettings = Seq(
  resolvers           += Resolver.sonatypeRepo("snapshots"),
  libraryDependencies += "de.sciss" % "submin" % deps.test.submin % Test
)

// ----

lazy val root = project.withId(baseNameL).in(file("."))
  .aggregate(core, swing, app)
  .dependsOn(core, swing, app)
  .settings(commonSettings)
  .settings(
    packagedArtifacts    := Map.empty,          // prevent publishing anything!
    mimaFailOnNoPrevious := false,
  )

lazy val core = project.withId(s"$baseNameL-core").in(file("core"))
  .settings(commonSettings)
  .settings(testSettings)
  .settings(
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-core" % mimaVersion)
  )

lazy val swing = project.withId(s"$baseNameL-swing").in(file("swing"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(testSettings)
  .settings(
    libraryDependencies ++= Seq(
      "de.sciss" %% "swingplus" % deps.main.swingPlus
    ),
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-swing" % mimaVersion)
  )

lazy val app = project.withId(s"$baseNameL-app").in(file("app"))
  .dependsOn(swing)
  .settings(commonSettings)
  .settings(testSettings)
  .settings(
    libraryDependencies ++= Seq(
      "de.sciss" %% "desktop"       % deps.main.desktop,
      "de.sciss" %% "raphael-icons" % deps.main.raphael,
      "de.sciss" %% "span"          % deps.main.span
    ),
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-app" % mimaVersion)
  )

// ---- publishing ----

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    )
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := { val n = baseName
<scm>
  <url>git@git.iem.at:sciss/{n}.git</url>
  <connection>scm:git:git@git.iem.at:sciss/{n}.git</connection>
</scm>
<developers>
   <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
   </developer>
</developers>
  }
)
