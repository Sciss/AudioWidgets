/*
 *  LCDPanel.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

import de.sciss.audiowidgets.j.{LCDPanel => JLCDPanel}

import scala.swing.FlowPanel

class LCDPanel extends FlowPanel {
  override lazy val peer: JLCDPanel = new JLCDPanel with SuperMixin
}