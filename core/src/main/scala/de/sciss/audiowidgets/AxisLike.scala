/*
 *  AxisLike.scala
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

package de.sciss.audiowidgets

trait AxisLike {
   def fixedBounds : Boolean
   def fixedBounds_= (b: Boolean): Unit
//   def format : Format
//   def format_= (f: Format): Unit
   def inverted : Boolean
   def inverted_= (b: Boolean): Unit
   def maximum : Double
   def maximum_= (value: Double): Unit
   def minimum : Double
   def minimum_= (value: Double): Unit
}

trait AxisCompanion {
   sealed trait Format
   object Format {
      case object Decimal extends Format
      case object Integer extends Format
      final case class Time( hours: Boolean = false, millis: Boolean = true ) extends Format
   }
}