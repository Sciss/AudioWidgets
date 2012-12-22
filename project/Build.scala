import sbt._
import Keys._
import sbtbuildinfo.Plugin._

object Build extends sbt.Build {
   lazy val audiowidgets: Project = Project(
      id        = "audiowidgets",
      base      = file( "." ),
      aggregate = Seq( core, swing )
   )

   lazy val core = Project(
      id        = "audiowidgets-core",
      base      = file( "core" ),
      settings     = Project.defaultSettings ++ buildInfoSettings ++ Seq(
//         scalaVersion := "2.10.0",
         // buildInfoSettings
         sourceGenerators in Compile <+= buildInfo,
         buildInfoKeys := Seq( name, organization, version, scalaVersion, description,
            BuildInfoKey.map( homepage ) { case (k, opt) => k -> opt.get },
            BuildInfoKey.map( licenses ) { case (_, Seq( (lic, _) )) => "license" -> lic }
         ),
         buildInfoPackage := "de.sciss.audiowidgets"
      )
   )

   lazy val swing = Project(
      id           = "audiowidgets-swing",
      base         = file( "swing" ),
      dependencies = Seq( core ),
      settings     = Project.defaultSettings ++ Seq(
//         scalaVersion := "2.10.0",
         libraryDependencies <+= scalaVersion { sv =>
            "org.scala-lang" % "scala-swing" % sv
         }
      )
   )
}
