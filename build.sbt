lazy val baseName = "AudioWidgets"

lazy val baseNameL = baseName.toLowerCase

version         in ThisBuild := "1.9.0"

organization    in ThisBuild := "de.sciss"

description     in ThisBuild := "Specialized Swing widgets for audio applications in Scala"

homepage        in ThisBuild := Some(url("https://github.com/Sciss/" + baseName))

licenses        in ThisBuild := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

scalaVersion    in ThisBuild := "2.11.5"

crossScalaVersions in ThisBuild := Seq("2.11.5", "2.10.4")

scalacOptions   in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture")

// ---- dependencies ----

lazy val desktopVersion = "0.7.0"

lazy val spanVersion    = "1.3.0"

lazy val raphaelVersion = "1.0.2"

// ---- test dependencies ----

lazy val webLaFVersion  = "1.28"

// ----

initialCommands in console in ThisBuild := """
  |import de.sciss.audiowidgets._""".stripMargin

lazy val audiowidgets: Project = Project(
  id        = baseNameL,
  base      = file("."),
  aggregate = Seq(core, swing, app),
  settings  = Project.defaultSettings ++ Seq(
    packagedArtifacts := Map.empty           // prevent publishing anything!
  )
)

lazy val core = Project(
  id        = s"$baseName-core",
  base      = file("core"),
  settings  = Project.defaultSettings ++ /* buildInfoSettings ++ */ Seq(
    libraryDependencies += "de.sciss" % "weblaf" % webLaFVersion % "test"
  )
)

lazy val swing = Project(
  id            = s"$baseNameL-swing",
  base          = file("swing"),
  dependencies  = Seq(core),
  settings      = Project.defaultSettings ++ Seq(
    libraryDependencies ++= { val sv = scalaVersion.value
      val swing = if (sv startsWith "2.11")
        "org.scala-lang.modules" %% "scala-swing" % "1.0.1"
      else
        "org.scala-lang" % "scala-swing" % sv
      Seq("de.sciss" % "weblaf" % webLaFVersion % "test", swing)
    }
  )
)

lazy val app = Project(
  id            = s"$baseNameL-app",
  base          = file("app"),
  dependencies  = Seq(swing),
  settings      = Project.defaultSettings ++ Seq(
    libraryDependencies ++= Seq(
      "de.sciss" %% "desktop"       % desktopVersion,
      "de.sciss" %% "raphael-icons" % raphaelVersion,
      "de.sciss" %% "span"          % spanVersion,
      "de.sciss" % "weblaf" % webLaFVersion % "test"
    )
  )
)

// ---- publishing ----

publishMavenStyle in ThisBuild := true

publishTo in ThisBuild :=
  Some(if (isSnapshot.value)
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )

publishArtifact in Test := false

pomIncludeRepository in ThisBuild := { _ => false }

pomExtra in ThisBuild := { val n = baseName
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

// ---- ls.implicit.ly ----

seq(lsSettings :_*)

(LsKeys.tags   in LsKeys.lsync) := Seq("swing", "audio", "widgets")

(LsKeys.ghUser in LsKeys.lsync) := Some("Sciss")

(LsKeys.ghRepo in LsKeys.lsync) := Some(baseName)
