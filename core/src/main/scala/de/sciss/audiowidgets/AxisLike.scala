/*
 *  AxisLike.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2016 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

trait AxisLike {
  var fixedBounds: Boolean
  var inverted   : Boolean

  var maximum: Double
  var minimum: Double
}

//trait AxisCompanion {
//   sealed trait Format
//   object Format {
//      case object Decimal extends Format
//      case object Integer extends Format
//      final case class Time( hours: Boolean = false, millis: Boolean = true ) extends Format
//   }
//}