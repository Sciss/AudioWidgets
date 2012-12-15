## AudioWidgets

### statement

AudioWidgets is a library providing specialized widgets for audio applications. It is Java-Swing based and written in the Scala programming language. (C)opyright 2011&ndash;2012 by Hanns Holger Rutz. All rights reserved. It is released under the [GNU General Public License](http://github.com/Sciss/AudioWidgets/blob/master/licenses/AudioWidgets-License.txt) and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`.

Note that there is a separate project to wrap this library into components suitable for Scala-Swing: [ScalaAudioWidgets](http://github.com/Sciss/ScalaAudioWidgets).

### requirements / installation

AudioWidgets currently compiles against Scala 2.9.2 and requires Java 1.6.

To use the library in your project:

    "de.sciss" %% "audiowidgets" % "1.0.+"

To view a demo of the widgets: `sbt test:run`.

### creating an IntelliJ IDEA project

If you want to develop the library, you can set up an IntelliJ IDEA project, using the sbt-idea plugin yet. Have the following contents in `~/.sbt/plugins/build.sbt`:

    addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.1.0")

Then to create the IDEA project, run `sbt gen-idea`.

### components

The following components are available:

 - `Axis` -- A general horizontal or vertical axis component
 - `LCDPanel` -- A JPanel with bevel border and glossy background color
 - `PeakMeter` -- A dual peak and RMS meter suitable for audio signals
 - `Transport` -- A tool bar for transport controls

The API docs are currently the only source of documentation. The sbt `run` target shows the available widgets.

### download

The current version can be downloaded from [github.com/Sciss/AudioWidgets](http://github.com/Sciss/AudioWidgets).
