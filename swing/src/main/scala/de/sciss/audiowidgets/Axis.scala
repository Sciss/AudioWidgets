/*
 *  Axis.scala
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

import j.{Axis => JAxis}
import scala.swing.{Font, Orientation, Component}

object Axis {
  def DefaultFont: Font = JAxis.DefaultFont
}
class Axis(orientation0: Orientation.Value = Orientation.Horizontal) extends Component with AxisLike {
  override lazy val peer: JAxis = new JAxis(orientation0.id) with SuperMixin

  def orientation        : Orientation.Value        = Orientation(peer.orientation)
  def orientation_=(value: Orientation.Value): Unit = peer.orientation = value.id

  def fixedBounds        : Boolean                  = peer.fixedBounds
  def fixedBounds_=(b    : Boolean          ): Unit = peer.fixedBounds = b

  def format             : AxisFormat               = peer.format
  def format_=     (f    : AxisFormat       ): Unit = peer.format = f

  def inverted           : Boolean                  = peer.inverted
  def inverted_=   (b    : Boolean          ): Unit = peer.inverted = b

  def maximum            : Double                   = peer.maximum
  def maximum_=    (value: Double           ): Unit = peer.maximum = value

  def minimum            : Double                   = peer.minimum
  def minimum_=    (value: Double           ): Unit = peer.minimum = value
}