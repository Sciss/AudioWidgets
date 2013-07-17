import sbt._
import Keys._
import sbtbuildinfo.Plugin._

object Build extends sbt.Build {
  def baseID = "audiowidgets"

  lazy val audiowidgets: Project = Project(
    id        = baseID,
    base      = file("."),
    aggregate = Seq(core, swing, app)
  )

  lazy val core = Project(
    id        = baseID + "-core",
    base      = file("core"),
    settings  = Project.defaultSettings ++ buildInfoSettings ++ Seq(
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
      libraryDependencies <+= scalaVersion { sv =>
        "org.scala-lang" % "scala-swing" % sv
      }
    )
  )

  lazy val app = Project(
    id            = baseID + "-app",
    base          = file("app"),
    dependencies  = Seq(swing),
    settings      = Project.defaultSettings ++ Seq(
      libraryDependencies ++= Seq(
        "de.sciss" %% "desktop"   % "0.3.2+",
        "de.sciss" %% "model"     % "0.3.1+",
        "de.sciss" %% "span"      % "1.2.+",
        "de.sciss" %% "swingplus" % "0.0.1+"      // until included in `desktop`
      )
    )
  )
}
