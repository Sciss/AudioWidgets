/*
 *  ParamField.scala
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

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.JFormattedTextField

import scala.collection.immutable.{Seq => ISeq}
import scala.swing.event.{SelectionChanged, ValueChanged}
import scala.swing.{Component, FormattedTextField}

/** A parameter field that combines a formatted text field and a combo-box for units.
  *
  * This widget fires `ValueChanged` for parameter value changed, and `SelectionChanged`
  * for selected format (unit) changes
  */
class ParamField[A](value0: A, formats0: ISeq[ParamFormat[A]]) extends Component with ParamFieldLike[A] { me =>
  override lazy val peer: j.ParamField[A] = new j.ParamField[A](value0, formats0) with SuperMixin { jp =>
    private[this] val pl = new PropertyChangeListener {
      private[this] var seenValue : A                       = jp.value
      private[this] var seenFormat: Option[ParamFormat[A]]  = jp.selectedFormat

      def propertyChange(evt: PropertyChangeEvent): Unit = {
        val pn = evt.getPropertyName
        if (pn == "value") {
          val newValue = jp.value
          if (seenValue != newValue) {
            seenValue = newValue
            publish(new ValueChanged(me))
          }
        } else if (pn == "selectedFormat") {
          val newFormat = jp.selectedFormat
          if (seenFormat != newFormat) {
            seenFormat = newFormat
            publish(SelectionChanged(me))
          }
        }
      }
    }
    jp.textField.addPropertyChangeListener("value", pl)
    jp.addPropertyChangeListener ("selectedFormat", pl)
  }

  lazy val textField: FormattedTextField = new FormattedTextField(null) {
    override lazy val peer: JFormattedTextField = me.peer.textField

  }

  def value: A = peer.value
  def value_=(a: A): Unit = peer.value = a

  def editable: Boolean = peer.editable
  def editable_=(value: Boolean): Unit = peer.editable = value

  def prototypeDisplayValues: ISeq[A] = peer.prototypeDisplayValues
  def prototypeDisplayValues_=(xs: ISeq[A]): Unit = peer.prototypeDisplayValues = xs

  def selectedFormat: Option[ParamFormat[A]] = peer.selectedFormat
  def selectedFormat_=(value: Option[ParamFormat[A]]): Unit = peer.selectedFormat = value

  def formats: ISeq[ParamFormat[A]] = peer.formats
  def formats_=(xs: ISeq[ParamFormat[A]]): Unit = peer.formats = xs
}
