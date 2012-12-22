name := "AudioWidgets"

version in ThisBuild := "1.1.0-SNAPSHOT"

organization in ThisBuild := "de.sciss"

description in ThisBuild := "Specialized Swing widgets for audio applications in Scala"

homepage in ThisBuild := Some( url( "https://github.com/Sciss/AudioWidgets" ))

licenses in ThisBuild := Seq( "GPL v2+" -> url( "http://www.gnu.org/licenses/gpl-2.0.txt" ))

scalaVersion in ThisBuild := "2.10.0"

crossScalaVersions in ThisBuild := Seq( "2.10.0", "2.9.2" )

retrieveManaged in ThisBuild := true

scalacOptions in ThisBuild ++= Seq( "-deprecation", "-unchecked" )

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

publishTo in ThisBuild <<= version { (v: String) =>
   Some( if( v.endsWith( "-SNAPSHOT" ))
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
   else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
   )
}

publishArtifact in Test := false

pomIncludeRepository in ThisBuild := { _ => false }

pomExtra in ThisBuild :=
<scm>
  <url>git@github.com:Sciss/AudioWidgets.git</url>
  <connection>scm:git:git@github.com:Sciss/AudioWidgets.git</connection>
</scm>
<developers>
   <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
   </developer>
</developers>

// ---- ls.implicit.ly ----

seq( lsSettings :_* )

(LsKeys.tags in LsKeys.lsync) := Seq( "swing", "audio", "widgets" )

(LsKeys.ghUser in LsKeys.lsync) := Some( "Sciss" )

(LsKeys.ghRepo in LsKeys.lsync) := Some( "AudioWidgets" )

// bug in ls -- doesn't find the licenses from global scope
(licenses in LsKeys.lsync) := Seq( "GPL v2+" -> url( "http://www.gnu.org/licenses/gpl-2.0.txt" ))
