/*
 *  PeakMeterChannel
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

trait PeakMeterChannel {
  var peak: Float
  def peakDecibels: Float

  /**
   * Reads or sets the linear mean square value. Not that this is
   * not the _root_ mean square for optimization purposes.
   * The caller needs to take the square root of the returned value.
   */
  var rms: Float
  def rmsDecibels: Float

  var hold: Float
  def holdDecibels: Float

  def clearHold (): Unit
  def clearMeter(): Unit
}