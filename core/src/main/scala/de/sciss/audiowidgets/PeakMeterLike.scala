/*
 *  PeakMeterLike.java
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2013 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

import collection.immutable.{IndexedSeq => IIdxSeq}

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
  def update(values: IIdxSeq[Float], offset: Int = 0, time: Long = System.currentTimeMillis): Boolean

  var borderVisible: Boolean

  var hasCaption    : Boolean
  var captionLabels : Boolean
  var captionVisible: Boolean
}
