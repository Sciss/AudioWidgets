/*
 *  TimelineModel.scala
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

import de.sciss.model.{Change, Model}
import de.sciss.span.Span
import de.sciss.span.Span.SpanOrVoid
import impl.{TimelineModelImpl => Impl}

object TimelineModel {
  sealed trait Update { def model: TimelineModel }
  final case class Visible  (model: TimelineModel, span:   Change[Span])       extends Update
  final case class Position (model: TimelineModel, frame:  Change[Long])       extends Update
  final case class Selection(model: TimelineModel, span:   Change[SpanOrVoid]) extends Update
  final case class Bounds   (model: TimelineModel, span:   Change[Span])       extends Update

  type Listener = Model.Listener[Update]

  trait Modifiable extends TimelineModel {
    var visible: Span
    var position: Long
    var selection: SpanOrVoid
    var bounds: Span

    def modifiableOption: Option[TimelineModel.Modifiable] = Some(this)
  }

  def apply(bounds: Span, sampleRate: Double): Modifiable = new Impl(bounds, sampleRate)
}

/** A `TimelineModel` encompasses the idea of a timeline based user interface.
  * Time is measured in sample frames. The timeline (currently) must have a closed
  * interval, `bounds`, and for editing and viewing contains a cursor (`position`) and
  * a `selection` span.
  *
  * A sub type, `TimelineModel.Modifiable`, allows to mutate any of these parameters.
  */
trait TimelineModel extends Model[TimelineModel.Update] {
  /** Sample rate in frames per seconds. */
  def sampleRate: Double

  /** The currently visible span in the user interface */
  def visible: Span
  /** The current cursor position in the user interface */
  def position: Long
  /** The current selection in the user interface */
  def selection: SpanOrVoid
  /** The timeline's total time span */
  def bounds: Span

  def modifiableOption: Option[TimelineModel.Modifiable]
}