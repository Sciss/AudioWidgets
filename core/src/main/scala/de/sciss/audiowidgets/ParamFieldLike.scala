/*
 *  ParamFieldLike.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2015 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

import scala.collection.immutable.{Seq => ISeq}

trait ParamFieldLike[A] {
  var value: A

  var formats: ISeq[ParamFormat[A]]

  var prototypeDisplayValues: ISeq[A]

  var selectedFormat: Option[ParamFormat[A]]

  var editable: Boolean
}
