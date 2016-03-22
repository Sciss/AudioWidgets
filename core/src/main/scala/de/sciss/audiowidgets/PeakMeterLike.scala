/*
 *  PeakMeterLike.java
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2016 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

import collection.immutable.{IndexedSeq => Vec}

trait PeakMeterLike {
  def clearHold (): Unit
  def clearMeter(): Unit

  def dispose(): Unit

  //   def holdDecibels : Float
  var holdDuration: Int
  var holdPainted: Boolean

  //   def orientation : Int
  //   def orientation_=( orient: Int ): Unit
  //   def peak : Float
  //   def peak_=( value: Float ) : Unit
  //   def peakDecibels : Float
  def channel(ch: Int): PeakMeterChannel

  var numChannels: Int
  //   def peak : Float
  //   def peak_=( value: Float ) : Unit
  //   def peakDecibels : Float
  //   var refreshParent : Boolean
  //   def rms : Float
  //   def rms_=( value: Float ) : Unit
  var rmsPainted: Boolean
  var ticks: Int

  //   def update( peak: Float, rms: Float = rms, time: Long = System.currentTimeMillis ) : Boolean
  def update(values: Vec[Float], offset: Int = 0, time: Long = System.currentTimeMillis): Boolean

  var borderVisible: Boolean

  var caption       : Boolean
  var captionLabels : Boolean
  var captionVisible: Boolean
}
