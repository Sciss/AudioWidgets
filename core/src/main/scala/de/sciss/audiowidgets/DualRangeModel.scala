/*
 *  DualRangeModel
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

import javax.swing.event.{ChangeEvent, ChangeListener}

import scala.collection.immutable.{IndexedSeq => Vec}

object DualRangeModel {
  def apply(minimum: Int = 0, maximum: Int = 100): DualRangeModel = new Impl(minimum, maximum)

  private final class Impl(private var _minimum: Int, private var _maximum: Int) extends DualRangeModel {
    private var _adjusting  = false
    private var _value      = _minimum
    private var _range      = (_minimum, _minimum)

    private val sync      = new AnyRef
    private var listeners = Vec.empty[ChangeListener]

    def addChangeListener   (l: ChangeListener): Unit = sync.synchronized(listeners :+= l)

    def removeChangeListener(l: ChangeListener): Unit = sync.synchronized {
      val idx = listeners.indexOf(l)
      if (idx >= 0) listeners = listeners.patch(idx, Vec.empty, 1)
    }

    private def fire(): Unit = {
      val ls = listeners
      if (ls.nonEmpty) {
        val evt = new ChangeEvent(this)
        ls.foreach(_.stateChanged(evt))
      }
    }

    def value: Int = _value

    def value_=(value: Int): Unit = if (setValue(value)) fire()

    private def clip(value: Int) = math.max(_minimum, math.min(_maximum, value))

    private def setValue(value: Int): Boolean = {
      val vc = clip(value)
      if (_value != vc) {
        _value = vc
        true
      } else false
    }

    def range: (Int, Int) = _range

    def range_=(value: (Int, Int)): Unit = if (setRange(value)) fire()

    private def setRange(value: (Int, Int)): Boolean = {
      // val lo0   = math.min(value._1, value._2)
      // val hi0   = math.max(value._1, value._2)
      val (lo0, hi0) = value
      val lo    = clip(lo0)
      val hi    = clip(hi0)
      val rc    = (lo, hi)
      if (_range != rc) {
        _range = rc
        true
      } else false
    }

    def minimum: Int = _minimum

    def minimum_=(value: Int): Unit = if (setMinimum(value)) fire()

    private def setMinimum(value: Int): Boolean =
      if (_minimum != value) {
        _minimum = value
        if (value > _maximum ) _maximum = value
        if (value > _value   ) _value   = value
        if (value > rangeLo  ) _range   = (clip(_range._1), clip(_range._2))

        true
      } else false

    def maximum: Int = _maximum

    def maximum_=(value: Int): Unit = if (setMaximum(value)) fire()

    private def setMaximum(value: Int): Boolean =
      if (_maximum != value) {
        _maximum = value

        if (value < _minimum ) _minimum = value
        if (value < _value   ) _value   = value
        if (value < rangeHi  ) _range   = (clip(_range._1), clip(_range._2))

        true
      } else false

    def adjusting: Boolean = _adjusting
    def adjusting_=(value: Boolean): Unit = if (setAdjusting(value)) fire()

    private def setAdjusting(value: Boolean): Boolean =
      if (_adjusting != value) {
        _adjusting = value
        true
      } else false

    def setRangeProperties(value: Int, range: (Int, Int), minimum: Int, maximum: Int, adjusting: Boolean): Unit = {
      val dirty = setMinimum  (minimum  ) |
                  setMaximum  (maximum  ) |
                  setRange    (range    ) |
                  setValue    (value    )
      if (dirty) {
        setAdjusting(adjusting)
        fire()
      }
    }
  }
}
trait DualRangeModel {
  /** Lower bound of the model. Changing this fires an event. */
  var minimum: Int
  /** Upper bound of the model. Changing this fires an event. */
  var maximum: Int
  /** Single value of the model. Changing this fires an event. */
  var value: Int
  /** Range value of the model. Changing this fires an event. */
  var range: (Int, Int)

  /** Logical range lower value, which is `min(range._1, range._2)`. */
  def rangeLo: Int = {
    val (r1, r2) = range
    math.min(r1, r2)
  }

  /** Logical range higher value, which is `max(range._1, range._2)`. */
  def rangeHi: Int = {
    val (r1, r2) = range
    math.max(r1, r2)
  }

  /** Flag to indicate whether user is currently dragging the slider.
    * Changing this fires an event.
    */
  var adjusting: Boolean

  /** Queries the extent of the range, which is `abs(range._2 - range._1)`. */
  def extent: Int = math.abs(range._2 - range._1)

  /** Adjusts the extent of the range, which is `abs(range._2 - range._1)`. Fires an event. */
  def extent_=(value: Int): Unit =
    range = (range._1, range._1 + value) // math.max(0, value)

  def addChangeListener   (l: ChangeListener): Unit
  def removeChangeListener(l: ChangeListener): Unit

  /** Adjusts all the properties. If any property constitutes a model change, fires an event.
    *
    * __Note:__  The `adjusting` parameter is treated specially: If it is the only property
    * that would change, then this method does nothing. For example, if the method is called
    * with default arguments, setting only `value = x` and `adjusting = true`, this performs
    * a check for `x`. If `x` is different from the current value, then both the model's
    * `value` and `adjusting` properties are set; otherwise nothing is changed (ignoring the
    * `adjusting` argument).
    */
  def setRangeProperties(value    : Int         = this.value,
                         range    : (Int, Int)  = this.range,
                         minimum  : Int         = this.minimum,
                         maximum  : Int         = this.maximum,
                         adjusting: Boolean     = this.adjusting): Unit
}
