/*
 *  ParamFormat.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2017 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

import javax.swing.JFormattedTextField

trait ParamFormat[A] {
  def unit: UnitView

  def format(value: A): String
  def parse(s: String): Option[A]

  def formatter: JFormattedTextField.AbstractFormatter

  def adjust(in: A, inc: Int): A
}