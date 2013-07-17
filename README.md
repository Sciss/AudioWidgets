# AudioWidgets

## statement

AudioWidgets is a library providing specialized widgets for audio applications. It is Swing based and written in the Scala programming language. (C)opyright 2011&ndash;2013 by Hanns Holger Rutz. All rights reserved. It is released under the [GNU General Public License](http://github.com/Sciss/AudioWidgets/blob/master/licenses/AudioWidgets-License.txt) and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`.

## requirements / installation

AudioWidgets currently compiles against Scala 2.10 using sbt 0.12. There are three sub projects: `core`, `swing` and `app`, where `swing` depends on `core` and `app` depends on `swing`.

 - The `core` project provides widgets based on plain Java Swing
 - `swing` wraps them for usage within Scala-Swing. Therefore, `swing` adds a further dependency on `scala-swing`.
 - `app` adds time based components for use in desktop applications. It also depends on the `desktop` library.

To use the library in your project:

    "de.sciss" %% "audiowidgets-core"  % v
    "de.sciss" %% "audiowidgets-swing" % v
    "de.sciss" %% "audiowidgets-app"   % v

The current stable version `v` is `"1.2.+"`

To view a demo of the widgets from the sbt console:

    > project audiowidgets-core
    > test:run
    
    > project audiowidgets-swing
    > test:run

## components

The following components are available:

 - `Axis` -- A general horizontal or vertical axis component
 - `LCDPanel` -- A JPanel with bevel border and glossy background color
 - `PeakMeter` -- A dual peak and RMS meter suitable for audio signals
 - `Transport` -- A tool bar for transport controls
 - `WavePainter` -- A painter class for waveforms, along with sub types for multi resolution display

The API docs are currently the only source of documentation (`sbt doc`).
