/*
 *  TimelineModelImpl.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2018 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets
package impl

import de.sciss.model.Change
import de.sciss.model.impl.ModelImpl
import de.sciss.span.Span.SpanOrVoid
import de.sciss.span.{Span, SpanLike}

import scala.math.{min, max}

final class TimelineModelImpl(bounds0: SpanLike, visible0: Span, virtual0: Span,
                              val sampleRate: Double, val clipStart: Boolean, val clipStop: Boolean)
  extends TimelineModel.Modifiable with ModelImpl[TimelineModel.Update] {

  import TimelineModel._

  private[this] var _total      = bounds0
  private[this] var _visible    = visible0
  private[this] var _virtual    = virtual0
  private[this] var _position   = bounds0.startOrElse(bounds0.clip(0L))
  private[this] var _selection  = Span.Void: SpanOrVoid

  def visible: Span = _visible
  def visible_=(value: Span): Unit = {
    val oldSpan = _visible
    if (oldSpan != value) {
      _visible = value
      val ch = Change(oldSpan, value)
      dispatch(Visible(this, ch))
    }
  }

  def virtual: Span = _virtual
  def virtual_=(value: Span): Unit = {
    val oldSpan = _virtual
    if (oldSpan != value) {
      _virtual = value
      val ch = Change(oldSpan, value)
      dispatch(Virtual(this, ch))
    }
  }

  def position: Long = _position
  def position_=(value: Long): Unit = {
    val oldPos = _position
    if (oldPos != value) {
      _position      = value
      val ch = Change(oldPos, value)
      dispatch(Position(this, ch))
    }
  }

  def selection: SpanOrVoid = _selection
  def selection_=(value: SpanOrVoid): Unit = {
    val oldSel = _selection
    if (oldSel != value) {
      _selection  = value
      val ch = Change(oldSel, value)
      dispatch(Selection(this, ch))
    }
  }

  def bounds: SpanLike = _total
  def bounds_=(value: SpanLike): Unit = {
    val oldTot = _total
    if (oldTot != value) {
      _total = value
      val ch = Change(oldTot, value)
      dispatch(Bounds(this, ch))
    }
  }

  def setVisibleExtendVirtual(v: Span): Unit = {
    if (!virtual.contains(v)) {
      virtual = virtual union v
    }
    visible = v
  }

  def setBoundsExtendVirtual(v: SpanLike): Unit = {
    if (!bounds.contains(v)) {
      bounds = bounds union v
    }
    bounds = v
  }

  def setVisibleReduceVirtual(v: Span): Unit = {
    visible = v
    if (virtual.start < v.start || virtual.stop > v.stop) {
      val start0  = min(v.start , _total    .startOrElse(v.start))
      val start   = min(start0  , _selection.startOrElse(start0 ))
      val stop0   = max(v.stop  , _total    .stopOrElse (v.stop ))
      val stop    = max(stop0   , _selection.stopOrElse (stop0  ))
      virtual = Span(start, stop)
    }
  }
}