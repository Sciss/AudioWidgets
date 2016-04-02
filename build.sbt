lazy val baseName       = "AudioWidgets"
lazy val baseNameL      = baseName.toLowerCase

lazy val projectVersion = "1.9.3"

lazy val commonSettings = Seq(
  version             := projectVersion,
  organization        := "de.sciss",
  description         := "Specialized Swing widgets for audio applications in Scala",
  homepage            := Some(url(s"https://github.com/Sciss/$baseName")),
  licenses            := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
  scalaVersion        := "2.11.8",
  crossScalaVersions  := Seq("2.11.8", "2.10.6"),
  scalacOptions      ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture"),
  initialCommands in console := """
    |import de.sciss.audiowidgets._""".stripMargin
) ++ publishSettings

// ---- dependencies ----

lazy val desktopVersion     = "0.7.2"
lazy val spanVersion        = "1.3.1"
lazy val raphaelVersion     = "1.0.3"
lazy val scalaSwingVersion  = "1.0.2"

// ---- test dependencies ----

lazy val subminVersion      = "0.2.0"

lazy val testSettings = Seq(
  libraryDependencies ++= Seq(
    "de.sciss" % "submin" % subminVersion % "test"
  )
)

// ----

lazy val root = Project(id = baseNameL, base = file(".")).
  aggregate(core, swing, app).
  settings(commonSettings).
  settings(
    packagedArtifacts := Map.empty           // prevent publishing anything!
  )

lazy val core = Project(id = s"$baseName-core", base = file("core"))
  .settings(commonSettings)
  .settings(testSettings)

lazy val swing = Project(id = s"$baseNameL-swing", base = file("swing"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(testSettings)
  .settings(
    libraryDependencies += { val sv = scalaVersion.value
      val swing = if (sv startsWith "2.11")
        "org.scala-lang.modules" %% "scala-swing" % scalaSwingVersion
      else
        "org.scala-lang" % "scala-swing" % sv
      swing
    }
  )

lazy val app = Project(id = s"$baseNameL-app", base = file("app"))
  .dependsOn(swing)
  .settings(commonSettings)
  .settings(testSettings)
  .settings(
    libraryDependencies ++= Seq(
      "de.sciss" %% "desktop"       % desktopVersion,
      "de.sciss" %% "raphael-icons" % raphaelVersion,
      "de.sciss" %% "span"          % spanVersion
    )
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
  <url>git@github.com:Sciss/{n}.git</url>
  <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
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