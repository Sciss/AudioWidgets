/*
 *  TransportCatch.scala
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

import de.sciss.model.Model

/** Functionality for transport scroll 'catch'.
  * When catch is enabled, the associated view is
  * supposed to scroll along with transport head.
  * UI elements can temporarily bypass such automatic
  * scrolling, by vetoing with `addCatchBypass`.
  */
trait TransportCatch extends Model[Boolean] {
  /** Adds a scroll veto. */
  def addCatchBypass(token: Any): Unit

  /** Removes a scroll veto. */
  def removeCatchBypass(token: Any): Unit

  var catchEnabled: Boolean

  /** Checks the conditions for scroll catching the transport
    * and activates the catch if necessary
    */
  def ensureCatch(): Unit
}
