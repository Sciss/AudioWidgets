## AudioWidgets

### statement

AudioWidgets is a library providing specialized widgets for audio applications. It is Swing based and written in the Scala programming language. (C)opyright 2011&ndash;2012 by Hanns Holger Rutz. All rights reserved. It is released under the [GNU General Public License](http://github.com/Sciss/AudioWidgets/blob/master/licenses/AudioWidgets-License.txt) and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`.

Note that there is a separate project to wrap this library into components suitable for Scala-Swing: [ScalaAudioWidgets](http://github.com/Sciss/ScalaAudioWidgets).

### requirements / installation

AudioWidgets currently compiles against Scala 2.10 (default) and 2.9.2 using sbt 0.12. There are two sub projects `core` and `swing`, where `swing` depends on `core`. The `core` project provides widgets based on plain Java Swing, whereas `swing` wraps them for usage within Scala-Swing. Therefore, `swing` adds a further dependency on `scala-swing`.

To use the library in your project:

    "de.sciss" %% "audiowidgets-core" % "1.1.+"

or

    "de.sciss" %% "audiowidgets-swing" % "1.1+"

To view a demo of the widgets from the sbt console:

    > project audiowidgets-core
    > test:run
    
    > project audiowidgets-swing
    > test:run

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
 - `WavePainter` -- A painter class for waveforms, along with sub types for multi resolution display

The API docs are currently the only source of documentation (`sbt doc`).

### download

The current version can be downloaded from [github.com/Sciss/AudioWidgets](http://github.com/Sciss/AudioWidgets).
