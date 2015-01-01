/*
 *  PeakMeter.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2015 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

import j.{PeakMeter => JPeakMeter}
import collection.immutable.{IndexedSeq => Vec}
import swing.{Alignment, Orientation, Component}

class PeakMeter extends Component with PeakMeterLike {
  override lazy val peer: JPeakMeter = new JPeakMeter with SuperMixin

  def numChannels            : Int                      = peer.numChannels
  def numChannels_=    (n    : Int              ): Unit = peer.numChannels = n

  def holdDuration           : Int                      = peer.holdDuration
  def holdDuration_=   (millis: Int             ): Unit = peer.holdDuration = millis

  def rmsPainted             : Boolean                  = peer.rmsPainted
  def rmsPainted_=     (b    : Boolean          ): Unit = peer.rmsPainted = b

  def holdPainted            : Boolean                  = peer.holdPainted
  def holdPainted_=    (b    : Boolean          ): Unit = peer.holdPainted = b

  def orientation            : Orientation.Value        = Orientation(peer.orientation)
  def orientation_=    (value: Orientation.Value): Unit = peer.orientation = value.id

  def ticks                  : Int                      = peer.ticks
  def ticks_=          (num  : Int              ): Unit = peer.ticks = num

  def caption                : Boolean                  = peer.caption
  def caption_=        (b    : Boolean          ): Unit = peer.caption = b

  def captionLabels          : Boolean                  = peer.captionLabels
  def captionLabels_=  (b    : Boolean          ): Unit = peer.captionLabels = b

  def captionVisible         : Boolean                  = peer.captionVisible
  def captionVisible_= (b    : Boolean          ): Unit = peer.captionVisible = b

  def captionPosition        : Alignment.Value          = Alignment(peer.captionPosition)
  def captionPosition_=(value: Alignment.Value  ): Unit = peer.captionPosition = value.id

  def borderVisible : Boolean = peer.borderVisible
  def borderVisible_=(b: Boolean): Unit = peer.borderVisible = b

  def clearHold (): Unit = peer.clearHold ()
  def clearMeter(): Unit = peer.clearMeter()
  def dispose   (): Unit = peer.dispose   ()

  def channel(ch: Int): PeakMeterChannel = peer.channel(ch)

  def update(peakRMSPairs: Vec[Float], offset: Int, time: Long) = peer.update(peakRMSPairs, offset, time)
}