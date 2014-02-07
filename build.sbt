lazy val baseName = "AudioWidgets"

version         in ThisBuild := "1.4.0"

organization    in ThisBuild := "de.sciss"

description     in ThisBuild := "Specialized Swing widgets for audio applications in Scala"

homepage        in ThisBuild := Some(url("https://github.com/Sciss/" + baseName))

licenses        in ThisBuild := Seq("GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt"))

scalaVersion    in ThisBuild := "2.10.3"

retrieveManaged in ThisBuild := true

scalacOptions   in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature")

initialCommands in console in ThisBuild := """
  |import de.sciss.audiowidgets._""".stripMargin

// ---- build info ----

// buildInfoSettings
// 
// sourceGenerators in Compile <+= buildInfo
// 
// buildInfoKeys := Seq( name, organization, version, scalaVersion, description,
//    BuildInfoKey.map( homepage ) { case (k, opt) => k -> opt.get },
//    BuildInfoKey.map( licenses ) { case (_, Seq( (lic, _) )) => "license" -> lic }
// )
// 
// buildInfoPackage := "de.sciss.gui.j"

// ---- publishing ----

publishMavenStyle in ThisBuild := true

publishTo in ThisBuild :=
  Some(if (version.value endsWith "-SNAPSHOT")
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
