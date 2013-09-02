package de.sciss.audiowidgets

import javax.swing.event.{ChangeEvent, ChangeListener}
import collection.immutable.{IndexedSeq => Vec}

object DualRangeModel {
  def apply(minimum: Int = 0, maximum: Int = 100): DualRangeModel = new Impl(minimum, maximum)

  private final class Impl(private var _minimum: Int, private var _maximum: Int) extends DualRangeModel {
    var valueIsAdjusting = false
    var rangeIsAdjusting = false

    private var _value  = _minimum
    private var _range  = (_minimum, _minimum)

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

    def value = _value

    def value_=(value: Int): Unit = {
      val clip = math.max(_minimum, math.min(_maximum, value))
      if (_value != clip) {
        _value = clip
        fire()
      }
    }

    def range = _range

    def range_=(value: (Int, Int)): Unit = {
      val lo    = math.max(_minimum, math.min(_maximum, value._1))
      val hi    = math.max(_minimum, math.min(_maximum, math.max(lo, value._2)))
      val clip  = (lo, hi)
      if (_range != clip) {
        _range = clip
        fire()
      }
    }

    def minimum = _minimum

    def minimum_=(value: Int): Unit =
      if (_minimum != value) {
        _minimum = value
        if (value > _maximum ) _maximum = value
        if (value > _value   ) _value   = value
        if (value > _range._1) _range   = (value, math.max(value, _range._2))

        fire()
      }

    def maximum = _maximum

    def maximum_=(value: Int): Unit =
      if (_maximum != value) {
        _maximum = value

        if (value < _minimum ) _minimum = value
        if (value < _value   ) _value   = value
        if (value < _range._2) _range   = (math.min(value, _range._1), value)

        fire()
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

  /** Flag to indicate whether user is currently dragging the slider. Changing this will not fire an event. */
  var valueIsAdjusting: Boolean

  /** Queries the extent of the range, which is `maximum - minimum`. */
  def extent: Int = range._2 - range._1

  /** Adjusts the extent of the range, which is `maximum - minimum`. Fires an event. */
  def extent_=(value: Int): Unit =
    range = (range._1, range._1 + math.max(0, value))

  def addChangeListener   (l: ChangeListener): Unit
  def removeChangeListener(l: ChangeListener): Unit
}
