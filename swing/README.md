## ScalaAudioWidgets

### statement

ScalaAudioWidgets provides Scala-Swing wrappers for the [AudioWidgets](http://github.com/Sciss/AudioWidgets) library. (C)opyright 2011&ndash;2012 by Hanns Holger Rutz. All rights reserved. It is released under the "GNU General Public License":http://github.com/Sciss/ScalaAudioWidgets/blob/master/licenses/ScalaAudioWidgets-License.txt and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`.

### requirements / installation

ScalaAudioWidgets currently compiles against Scala 2.9.2 and requires Java 1.6. It builds with sbt 0.12.0.

To use the library in your project:

    "de.sciss" %% "scalaaudiowidgets" % "1.0.+"

To run the demo: `sbt test:run`

### creating an IntelliJ IDEA project

If you want to develop the library, you can set up an IntelliJ IDEA project, using the sbt-idea plugin yet. Have the following contents in `~/.sbt/plugins/build.sbt`:

    resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"

    addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.0.0")

Then to create the IDEA project, run `sbt gen-idea`.

### download

The current version can be downloaded from [github.com/Sciss/ScalaAudioWidgets](http://github.com/Sciss/ScalaAudioWidgets).
