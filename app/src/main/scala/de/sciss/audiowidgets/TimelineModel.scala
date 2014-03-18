/*
 *  TimelineModel.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v3+
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