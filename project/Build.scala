import sbt._
import Keys._
import sbtbuildinfo.Plugin._

object Build extends sbt.Build {
  def baseID = "audiowidgets"

  lazy val desktopVersion = "0.6.0"
  lazy val spanVersion    = "1.2.1"
  lazy val raphaelVersion = "1.0.2"
  lazy val webLaFVersion  = "1.28"

  lazy val audiowidgets: Project = Project(
    id        = baseID,
    base      = file("."),
    aggregate = Seq(core, swing, app),
    settings  = Project.defaultSettings ++ Seq(
      packagedArtifacts := Map.empty           // prevent publishing anything!
    )
  )

  lazy val core = Project(
    id        = baseID + "-core",
    base      = file("core"),
    settings  = Project.defaultSettings ++ buildInfoSettings ++ Seq(
      libraryDependencies += "de.sciss" % "weblaf" % webLaFVersion % "test",
      //         scalaVersion := "2.10.0",
      // buildInfoSettings
      sourceGenerators in Compile <+= buildInfo,
      buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
        BuildInfoKey.map(homepage) {
          case (k, opt) => k -> opt.get
        },
        BuildInfoKey.map(licenses) {
          case (_, Seq((lic, _))) => "license" -> lic
        }
      ),
      buildInfoPackage := "de.sciss.audiowidgets"
    )
  )

  lazy val swing = Project(
    id            = baseID + "-swing",
    base          = file("swing"),
    dependencies  = Seq(core),
    settings      = Project.defaultSettings ++ Seq(
      libraryDependencies ++= { val sv = scalaVersion.value
        val swing = if (sv startsWith "2.11")
          "org.scala-lang.modules" %% "scala-swing" % "1.0.1"
        else
          "org.scala-lang" % "scala-swing" % sv
        Seq(
          "de.sciss" % "weblaf" % webLaFVersion % "test",
          swing
        )
      }
    )
  )

  lazy val app = Project(
    id            = baseID + "-app",
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
}
