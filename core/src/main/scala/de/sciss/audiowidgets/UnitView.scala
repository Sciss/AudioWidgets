/*
 *  UnitView.scala
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

import javax.swing.Icon

object UnitView {
  def apply(name: String, label: String            ): UnitView = new Impl(name, Some(label), None      )
  def apply(name: String,                icon: Icon): UnitView = new Impl(name, None       , Some(icon))
  def apply(name: String, label: String, icon: Icon): UnitView = new Impl(name, Some(label), Some(icon))

  def empty: UnitView = new Impl("none", None, None)

  private final class Impl(val name: String, val label: Option[String], val icon: Option[Icon]) extends UnitView {
    override def toString = s"UnitView($name${label.fold("")(s => s", label = $s")})"
  }
}
trait UnitView {
  def label: Option[String]
  def icon : Option[Icon]
  
  def name : String
}