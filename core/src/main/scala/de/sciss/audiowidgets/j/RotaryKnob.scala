/*
 *  RotaryKnob.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets
package j

import java.awt.Color
import javax.swing.{BoundedRangeModel, DefaultBoundedRangeModel, JSlider}

import de.sciss.audiowidgets.j.ui.RotaryKnobUI

class RotaryKnob(m: BoundedRangeModel) extends JSlider(m) with RotaryKnobLike {
  private var colrKnob        : Color   = _
  private var colrHand        : Color   = _
  private var colrRange       : Color   = _
  private var colrTrack       : Color   = _
  private var _centered       : Boolean = false

  def this(min: Int, max: Int, value: Int) = {
    this(new DefaultBoundedRangeModel(value, 0, min, max))
  }

  def this(min: Int, max: Int) = {
    this(min, max, (min + max) / 2)
  }

  def this() = {
    this(0, 100, 50)
  }

  def centered: Boolean = _centered

  def centered_=(value: Boolean): Unit =
    if (_centered != value) {
      _centered = value
      firePropertyChange("centered", !value, value)
    }

  def knobColor: Color = colrKnob

  def knobColor_=(value: Color): Unit = {
    if ((colrKnob == null && value != null) || (colrKnob != null && !(colrKnob == value))) {
      colrKnob = value
      repaint()
    }
  }

  def handColor: Color = colrHand

  def handColor_=(value: Color): Unit = {
    if ((colrHand == null && value != null) || (colrHand != null && !(colrHand == value))) {
      colrHand = value
      repaint()
    }
  }

  def rangeColor: Color = colrRange

  def rangeColor_=(value: Color): Unit = {
    if ((colrRange == null && value != null) || (colrRange != null && !(colrRange == value))) {
      colrRange = value
      repaint()
    }
  }

  def trackColor: Color = colrTrack

  def trackColor_=(value: Color): Unit = {
    if ((colrTrack == null && value != null) || (colrTrack != null && !(colrTrack == value))) {
      colrTrack = value
      repaint()
    }
  }

  override def updateUI(): Unit = {
    setUI(new RotaryKnobUI(this))
    updateLabelUIs()
  }
}