/*
 *  InstallableAction.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets.j

import javax.swing.{JComponent, Action}

trait InstallableAction extends Action {
  def install(component: JComponent, condition: Int = JComponent.WHEN_IN_FOCUSED_WINDOW): Unit
}