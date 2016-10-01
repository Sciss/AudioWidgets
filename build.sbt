name := "AudioWidgets"
version      := "0.13"
organization := "de.sciss"
description  := "Specialized Swing widgets for audio applications in Scala"
homepage     := Some(url("https://github.com/Sciss/AudioWidgets"))
licenses     := Seq("GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt"))
scalaVersion := "2.11.8"

scalacOptions ++= Seq("-deprecation", "-unchecked")

// ---- publishing ----

publishMavenStyle := true

publishTo :=
   Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
   else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
   )

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra :=
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
