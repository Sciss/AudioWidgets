lazy val baseName       = "AudioWidgets"
lazy val baseNameL      = baseName.toLowerCase

lazy val projectVersion = "2.3.2"
lazy val mimaVersion    = "2.3.0" // used for migration-manager

// sonatype plugin requires that these are in global
ThisBuild / version      := projectVersion
ThisBuild / organization := "de.sciss"

lazy val commonSettings = Seq(
//  version             := projectVersion,
//  organization        := "de.sciss",
  description         := "Specialized Swing widgets for audio applications in Scala",
  homepage            := Some(url(s"https://git.iem.at/sciss/$baseName")),
  licenses            := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
  scalaVersion        := "2.13.4",
  crossScalaVersions  := Seq("3.0.0", "2.13.4", "2.12.13"),
  scalacOptions      ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xlint", "-Xsource:2.13"),
  initialCommands in console := """
    |import de.sciss.audiowidgets._""".stripMargin
) ++ publishSettings

// ---- dependencies ----

lazy val deps = new {
  val main = new {
    val desktop     = "0.11.3"
    val span        = "2.0.2"
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
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  developers := List(
    Developer(
      id    = "sciss",
      name  = "Hanns Holger Rutz",
      email = "contact@sciss.de",
      url   = url("https://www.sciss.de")
    )
  ),
  scmInfo := {
    val h = "git.iem.at"
    val a = s"sciss/$baseName"
    Some(ScmInfo(url(s"https://$h/$a"), s"scm:git@$h:$a.git"))
  },
)

