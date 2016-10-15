# AudioWidgets

[![Build Status](https://travis-ci.org/Sciss/AudioWidgets.svg?branch=master)](https://travis-ci.org/Sciss/AudioWidgets)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.sciss/audiowidgets-core_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.sciss/audiowidgets-core_2.11)

## statement

AudioWidgets is a library providing specialized widgets for audio applications. It is Swing based and written in the Scala programming language. (C)opyright 2011&ndash;2016 by Hanns Holger Rutz. All rights reserved. It is released under the [GNU Lesser General Public License](http://github.com/Sciss/AudioWidgets/blob/master/licenses/AudioWidgets-License.txt) v2.1+ and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`.

The included font "Familida Mono" is based on a font by darunio123456789 and released under a [Creative Commons CC BY-SA 3.0 license](http://github.com/Sciss/AudioWidgets/blob/master/licenses/FamiliadaMono-License.txt).

## requirements / installation

AudioWidgets currently compiles against Scala 2.11, 2.10 using sbt 0.13. There are three sub projects: `core`, `swing` and `app`, where `swing` depends on `core` and `app` depends on `swing`.

 - The `core` project provides widgets based on plain Java Swing
 - `swing` wraps them for usage within Scala-Swing. Therefore, `swing` adds a further dependency on `scala-swing`.
 - `app` adds time based components for use in desktop applications. It also depends on the `desktop` library.

To use the library in your project:

    "de.sciss" %% "audiowidgets-core"  % v
    "de.sciss" %% "audiowidgets-swing" % v
    "de.sciss" %% "audiowidgets-app"   % v

The current version `v` is `"1.10.1"`.

To view a demo of the widgets from the sbt console:

    > project audiowidgets-core
    > test:run
    
    > project audiowidgets-swing
    > test:run

    > project audiowidgets-app
    > test:run

## components

The following components are available:

 - `Axis` -- A general horizontal or vertical axis component
 - `LCDPanel` -- A JPanel with bevel border and glossy background color
 - `PeakMeter` -- A dual peak and RMS meter suitable for audio signals
 - `Transport` -- A tool bar for transport controls
 - `WavePainter` -- A painter class for waveforms, along with sub types for multi resolution display
 - `DualRangeSlider` -- A QuickTime Player style slider combining single value and a range thumb
 - `RotaryKnob` -- A single value slider styled as a rotary knob

The API docs are currently the only source of documentation (`sbt doc`).
