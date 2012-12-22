/*
 *  Axis.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2012 Hanns Holger Rutz. All rights reserved.
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

package de.sciss.gui

import j.{Axis => JAxis, AxisCompanion, AxisLike}
import swing.Component
import JAxis.{Format => JFormat}

object Axis extends AxisCompanion
class Axis extends Component with AxisLike {
   import Axis._

   override lazy val peer: JAxis = new JAxis with SuperMixin
   def fixedBounds = peer.fixedBounds
   def fixedBounds_=( b: Boolean ) { peer.fixedBounds = b }

   // stupid translations...
   def format : Format = peer.format match {
      case JFormat.Decimal => Format.Decimal
      case JFormat.Integer => Format.Integer
      case JFormat.Time( hours, millis ) => Format.Time( hours, millis )
   }

   // stupid translations...
   def format_=( f: Format ) { peer.format = f match {
      case Format.Decimal => JFormat.Decimal
      case Format.Integer => JFormat.Integer
      case Format.Time( hours, millis ) => JFormat.Time( hours, millis )
   }}

   def inverted = peer.inverted
   def inverted_=( b: Boolean ) { peer.inverted = b }
   def maximum = peer.maximum
   def maximum_=( value: Double ) { peer.maximum = value }
   def minimum = peer.minimum
   def minimum_=( value: Double ) { peer.minimum = value }
}