/*
 *  RotaryKnob.scala
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

import scala.swing.Slider
import javax.swing.{DefaultBoundedRangeModel, BoundedRangeModel, JSlider}
import java.awt.Color

class RotaryKnob(model0: BoundedRangeModel) extends Slider with RotaryKnobLike {
  def this() {
    this(new DefaultBoundedRangeModel(50, 0, 0, 100))
  }

  override lazy val peer: j.RotaryKnob = new j.RotaryKnob(model0) with SuperMixin

  def knobColor         : Color         = peer.knobColor
  def knobColor_= (value: Color): Unit  = peer.knobColor  = value

  def handColor         : Color         = peer.handColor
  def handColor_= (value: Color): Unit  = peer.handColor  = value

  def rangeColor        : Color         = peer.rangeColor
  def rangeColor_=(value: Color): Unit  = peer.rangeColor = value

  def trackColor        : Color         = peer.trackColor
  def trackColor_=(value: Color): Unit  = peer.trackColor = value

  def centered        : Boolean         = peer.centered
  def centered_=(value: Boolean): Unit  = peer.centered   = value
}
