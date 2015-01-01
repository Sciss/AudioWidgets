/*
 *  DualRangeSlider.java
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
package j

import javax.swing.{SwingConstants, JComponent}
import javax.swing.event.{ChangeEvent, ChangeListener}
import scala.collection.immutable.{IndexedSeq => Vec}
import de.sciss.audiowidgets.j.ui.DualRangeSliderUI

/** A slider similar to the one found in QuickTime Player 7. It combines a linear positioning slider
  * with a range (region) slider.
  */
class DualRangeSlider(model0: DualRangeModel) extends JComponent with DualRangeSliderLike {
  private var _valueVisible = true
  private var _rangeVisible = true
  private var _orientation  = SwingConstants.HORIZONTAL

  var valueEditable = true
  var rangeEditable = true
  var extentFixed   = false

  def valueVisible  = _valueVisible
  def valueVisible_=(value: Boolean): Unit =
    if (_valueVisible != value) {
      _valueVisible = value
      repaint()
    }

  def rangeVisible  = _rangeVisible
  def rangeVisible_=(value: Boolean): Unit =
    if (_rangeVisible != value) {
      _rangeVisible = value
      repaint()
    }

  private var _model = model0

  private val sync      = new AnyRef
  private var listeners = Vec.empty[ChangeListener]

  private val listener = new ChangeListener {
    def stateChanged(e: ChangeEvent): Unit = listeners.foreach(_.stateChanged(e)) // forward
  }

  def model = _model
  def model_=(value: DualRangeModel): Unit = {
    _model.removeChangeListener(listener)
    _model = value
    _model.addChangeListener(listener)
  }

  def addChangeListener   (l: ChangeListener): Unit = sync.synchronized(listeners :+= l)

  def removeChangeListener(l: ChangeListener): Unit = sync.synchronized {
    val idx = listeners.indexOf(l)
    if (idx >= 0) listeners = listeners.patch(idx, Vec.empty, 1)
  }

  override def updateUI(): Unit = setUI(new DualRangeSliderUI(this))

  def orientation: Int = _orientation

  def orientation(value: Int): Unit = {
    if (value == _orientation) return

    if (value != SwingConstants.VERTICAL && value != SwingConstants.HORIZONTAL)
      throw new IllegalArgumentException("orientation must be one of: VERTICAL, HORIZONTAL")

    val old = _orientation
    _orientation = value
    firePropertyChange("orientation", old, value)

    // if (accessibleContext != null) ...

    revalidate()
    updateUI()
  }

  // ---- constructor ----

  model0.addChangeListener(listener)
  setFocusable(true)
  updateUI()
}
