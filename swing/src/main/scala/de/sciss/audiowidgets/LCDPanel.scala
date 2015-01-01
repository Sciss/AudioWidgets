/*
 *  LCDPanel.scala
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

import j.{LCDPanel => JLCDPanel}
import swing.FlowPanel

class LCDPanel extends FlowPanel {
  override lazy val peer: JLCDPanel = new JLCDPanel with SuperMixin
}